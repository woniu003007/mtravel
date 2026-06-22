package com.mtravel.platform.purchase.relation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationResponse;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSaveRequest;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 采购关系管理服务。
 *
 * <p>采购关系用于维护资源和供应商之间的绑定关系。具体价格进入价格管理明细表；
 * 列表展示所在地、供应商负责人和电话，与旧系统字段保持一致。</p>
 */
@Service
public class PurchaseRelationService extends BusinessCrudService<PurchaseRelationEntity, PurchaseRelationResponse> {

    private final PurchaseRelationMapper mapper;
    private final SupplierLookupService supplierLookup;
    private final PurchaseResourceMapper resourceMapper;
    private final SupplierMapper supplierMapper;

    public PurchaseRelationService(
            PurchaseRelationMapper mapper,
            SupplierLookupService supplierLookup,
            PurchaseResourceMapper resourceMapper,
            SupplierMapper supplierMapper
    ) {
        super(mapper);
        this.mapper = mapper;
        this.supplierLookup = supplierLookup;
        this.resourceMapper = resourceMapper;
        this.supplierMapper = supplierMapper;
    }

    /** 分页查询采购关系，支持按资源名称、资源类型、状态和供应商筛选。 */
    public PageResult<PurchaseRelationResponse> page(
            Long tenantId,
            String keyword,
            String resourceType,
            String status,
            Long supplierId,
            long page,
            long pageSize
    ) {
        QueryWrapper<PurchaseRelationEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(supplierId != null, "supplier_id", supplierId)
                .like(StringUtils.hasText(keyword), "resource_name", keyword)
                .orderByAsc("resource_type")
                .orderByAsc("resource_name")
                .orderByDesc("id");
        Page<PurchaseRelationEntity> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        List<PurchaseRelationEntity> records = result.getRecords();
        Map<Long, PurchaseResourceEntity> resources = resourcesForDisplay(tenantId, records);
        Map<Long, SupplierEntity> suppliers = suppliersForDisplay(tenantId, records);
        List<PurchaseRelationResponse> items = records.stream()
                .map(item -> PurchaseRelationResponse.fromEntities(
                        item,
                        resources.get(item.getResourceId()),
                        suppliers.get(item.getSupplierId())
                ))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 新增采购关系。资源类型和名称从资源主档带出，防止前端手工写错。 */
    public PurchaseRelationResponse create(PurchaseRelationSaveRequest request, Long tenantId, String operator) {
        supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        PurchaseResourceEntity resource = resource(tenantId, request.resourceId());
        assertUnique(tenantId, request.resourceId(), request.supplierId(), number(request.groupQuantity()), null);

        PurchaseRelationEntity entity = new PurchaseRelationEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request, resource);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改采购关系。修改时允许更换资源和供应商，成团数量保留为历史兼容字段。 */
    public PurchaseRelationResponse update(Long id, PurchaseRelationSaveRequest request, Long tenantId) {
        supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        PurchaseResourceEntity resource = resource(tenantId, request.resourceId());
        assertUnique(tenantId, request.resourceId(), request.supplierId(), number(request.groupQuantity()), id);

        PurchaseRelationEntity entity = new PurchaseRelationEntity();
        applyFields(entity, request, resource);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 将资源主档和请求字段合并写入采购关系。 */
    private void applyFields(
            PurchaseRelationEntity entity,
            PurchaseRelationSaveRequest request,
            PurchaseResourceEntity resource
    ) {
        entity.setResourceType(resource.getResourceType());
        entity.setResourceId(resource.getId());
        entity.setResourceName(resource.getResourceName());
        entity.setSupplierId(request.supplierId());
        entity.setGroupQuantity(number(request.groupQuantity()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(clean(request.remark()));
    }

    /** 查询同租户未删除资源，采购关系只能绑定真实资源主档。 */
    private PurchaseResourceEntity resource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity entity = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId));
        if (entity == null) {
            throw new BizException("绑定资源不存在或已删除");
        }
        return entity;
    }

    /** 同一资源、供应商和成团数量下只能有一条未删除关系。 */
    private void assertUnique(
            Long tenantId,
            Long resourceId,
            Long supplierId,
            Integer groupQuantity,
            Long excludeId
    ) {
        QueryWrapper<PurchaseRelationEntity> wrapper = baseQuery(tenantId)
                .eq("resource_id", resourceId)
                .eq("supplier_id", supplierId)
                .eq("group_quantity", number(groupQuantity))
                .ne(excludeId != null, "id", excludeId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("采购关系已存在");
        }
    }

    @Override
    protected PurchaseRelationEntity newEntity() {
        return new PurchaseRelationEntity();
    }

    @Override
    protected PurchaseRelationResponse toResponse(PurchaseRelationEntity entity) {
        return PurchaseRelationResponse.fromEntities(
                entity,
                resourceForDisplay(entity.getTenantId(), entity.getResourceId()),
                supplierLookup.supplier(entity.getTenantId(), entity.getSupplierId())
        );
    }

    /**
     * 查询采购关系对应的资源主档，用于返回所在地。
     *
     * <p>历史资源被软删除时返回 {@code null}，采购关系仍可正常展示，不因附加字段缺失而报错。</p>
     */
    private PurchaseResourceEntity resourceForDisplay(Long tenantId, Long resourceId) {
        return resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId));
    }

    /**
     * 批量加载当前页采购关系对应的资源主档。
     *
     * <p>采购关系页和团队安排弹窗会一次拉取 200 条关系。如果每行再查一次资源所在地，
     * 会形成明显 N+1 查询；这里统一批量查询并按 ID 建索引。</p>
     */
    private Map<Long, PurchaseResourceEntity> resourcesForDisplay(Long tenantId, List<PurchaseRelationEntity> records) {
        List<Long> resourceIds = records.stream()
                .map(PurchaseRelationEntity::getResourceId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        return resourceMapper.selectList(new QueryWrapper<PurchaseResourceEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("id", resourceIds))
                .stream()
                .collect(Collectors.toMap(PurchaseResourceEntity::getId, Function.identity(), (left, right) -> left));
    }

    /** 批量加载当前页采购关系对应的供应商档案，避免逐行查询供应商名称、负责人和电话。 */
    private Map<Long, SupplierEntity> suppliersForDisplay(Long tenantId, List<PurchaseRelationEntity> records) {
        List<Long> supplierIds = records.stream()
                .map(PurchaseRelationEntity::getSupplierId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        return supplierMapper.selectList(new QueryWrapper<SupplierEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("id", supplierIds))
                .stream()
                .collect(Collectors.toMap(SupplierEntity::getId, Function.identity(), (left, right) -> left));
    }

    @Override
    protected String notFoundMessage() {
        return "采购关系不存在或已删除";
    }
}
