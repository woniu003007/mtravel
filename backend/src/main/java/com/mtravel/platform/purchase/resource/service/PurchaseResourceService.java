package com.mtravel.platform.purchase.resource.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceBindingResponse;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceResponse;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceSaveRequest;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceStatus;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceType;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 采购资源总览服务。
 *
 * <p>本服务维护资源主档，并负责处理资源和供应商的轻量绑定入口。资源本身不保存采购价，
 * 采购关系只保存绑定和成团数量，具体价格进入供应商资源价格表。</p>
 */
@Service
public class PurchaseResourceService extends BusinessCrudService<PurchaseResourceEntity, PurchaseResourceResponse> {

    private final PurchaseResourceMapper resourceMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseRelationMapper relationMapper;

    public PurchaseResourceService(
            PurchaseResourceMapper resourceMapper,
            SupplierMapper supplierMapper,
            PurchaseRelationMapper relationMapper
    ) {
        super(resourceMapper);
        this.resourceMapper = resourceMapper;
        this.supplierMapper = supplierMapper;
        this.relationMapper = relationMapper;
    }

    /**
     * 分页查询资源总览。
     *
     * <p>查询条件保持旧系统页面的核心筛选口径：资源类型、地区和资源名称。状态用于新系统启停管理，
     * 不改变资源总览的主业务含义。</p>
     */
    public PageResult<PurchaseResourceResponse> page(
            Long tenantId,
            String keyword,
            String resourceType,
            String province,
            String city,
            String district,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<PurchaseResourceEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                .eq(StringUtils.hasText(province), "province", province)
                .eq(StringUtils.hasText(city), "city", city)
                .eq(StringUtils.hasText(district), "district", district)
                .eq(StringUtils.hasText(status), "status", status)
                .like(StringUtils.hasText(keyword), "resource_name", keyword)
                .orderByDesc("id");
        Page<PurchaseResourceEntity> result = resourceMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<PurchaseResourceEntity> records = result.getRecords();
        Map<Long, Long> boundCountMap = boundSupplierCountMap(tenantId, records);
        List<PurchaseResourceResponse> items = records.stream()
                .map(item -> PurchaseResourceResponse.fromEntity(item, boundCountMap.get(item.getId())))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 新增资源。
     *
     * <p>勾选自动创建同名供应商时，系统会优先复用同租户、同名、同分类的供应商；没有则创建供应商，
     * 然后写入一条默认采购关系，保证“已绑定 X 家”立即能反映出来。</p>
     */
    @Transactional
    public PurchaseResourceResponse create(PurchaseResourceSaveRequest request, Long tenantId, String operator) {
        assertUnique(request, tenantId, null);

        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        resourceMapper.insert(entity);

        if (Boolean.TRUE.equals(request.autoCreateSupplier())) {
            SupplierEntity supplier = findOrCreateSameNameSupplier(entity, tenantId, operator);
            ensureRelation(entity, supplier.getId(), tenantId, operator);
        }
        return toResponse(entity);
    }

    /** 修改资源主档。修改资源不会自动改历史采购关系，避免误影响已有采购价格和合同口径。 */
    public PurchaseResourceResponse update(Long id, PurchaseResourceSaveRequest request, Long tenantId) {
        assertUnique(request, tenantId, id);

        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        applyFields(entity, request);
        int updated = resourceMapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 查询某个资源已经绑定的供应商列表。 */
    public List<PurchaseResourceBindingResponse> bindings(Long resourceId, Long tenantId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(baseQuery(tenantId).eq("id", resourceId));
        if (resource == null) {
            throw new BizException(notFoundMessage());
        }
        return relationMapper.selectList(new QueryWrapper<PurchaseRelationEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_type", resource.getResourceType())
                        .eq("resource_id", resource.getId())
                        .orderByAsc("group_quantity")
                        .orderByDesc("id"))
                .stream()
                .map(item -> PurchaseResourceBindingResponse.fromEntity(item, supplierName(tenantId, item.getSupplierId())))
                .toList();
    }

    /** 将接口字段清洗后写入实体，防止空字符串进入基础资料表。 */
    private void applyFields(PurchaseResourceEntity entity, PurchaseResourceSaveRequest request) {
        String resourceType = cleanRequired(request.resourceType());
        if (!PurchaseResourceType.contains(resourceType)) {
            throw new BizException("资源类型不合法");
        }
        entity.setResourceType(resourceType);
        entity.setResourceName(cleanRequired(request.resourceName()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setPhone(clean(request.phone()));
        entity.setFax(clean(request.fax()));
        entity.setAddress(clean(request.address()));
        entity.setWarmTip(request.warmTip());
        entity.setIntroduction(request.introduction());
        entity.setStatus(StringUtils.hasText(request.status())
                ? request.status()
                : PurchaseResourceStatus.ACTIVE.value());
        entity.setRemark(request.remark());
    }

    /** 同一租户、同类型、同地区下的未删除资源名称不能重复。 */
    private void assertUnique(PurchaseResourceSaveRequest request, Long tenantId, Long excludeId) {
        QueryWrapper<PurchaseResourceEntity> wrapper = baseQuery(tenantId)
                .eq("resource_type", request.resourceType())
                .eq("resource_name", cleanRequired(request.resourceName()))
                .eq(StringUtils.hasText(request.province()), "province", request.province())
                .isNull(!StringUtils.hasText(request.province()), "province")
                .eq(StringUtils.hasText(request.city()), "city", request.city())
                .isNull(!StringUtils.hasText(request.city()), "city")
                .eq(StringUtils.hasText(request.district()), "district", request.district())
                .isNull(!StringUtils.hasText(request.district()), "district")
                .ne(excludeId != null, "id", excludeId);
        Long count = resourceMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("同地区同类型资源名称已存在");
        }
    }

    /** 查找或创建同名供应商，用于资源创建时的快捷绑定。 */
    private SupplierEntity findOrCreateSameNameSupplier(PurchaseResourceEntity resource, Long tenantId, String operator) {
        SupplierEntity existing = supplierMapper.selectOne(new QueryWrapper<SupplierEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("supplier_name", resource.getResourceName())
                .eq("supplier_category", resource.getResourceType())
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        SupplierEntity supplier = new SupplierEntity();
        supplier.setTenantId(tenantId);
        supplier.setSupplierName(resource.getResourceName());
        supplier.setSupplierCategory(resource.getResourceType());
        supplier.setProvince(resource.getProvince());
        supplier.setCity(resource.getCity());
        supplier.setDistrict(resource.getDistrict());
        supplier.setContactPhone(resource.getPhone());
        supplier.setStatus("active");
        supplier.setCreatedBy(operator);
        supplier.setRemark("由资源总览自动创建");
        supplier.setIsDeleted(false);
        supplierMapper.insert(supplier);
        return supplier;
    }

    /** 确保资源和供应商之间存在一条默认采购关系。 */
    private void ensureRelation(PurchaseResourceEntity resource, Long supplierId, Long tenantId, String operator) {
        Long count = relationMapper.selectCount(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_type", resource.getResourceType())
                .eq("resource_id", resource.getId())
                .eq("supplier_id", supplierId));
        if (count != null && count > 0) {
            return;
        }

        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setTenantId(tenantId);
        relation.setResourceType(resource.getResourceType());
        relation.setResourceId(resource.getId());
        relation.setResourceName(resource.getResourceName());
        relation.setSupplierId(supplierId);
        relation.setGroupQuantity(0);
        relation.setStatus("active");
        relation.setCreatedBy(operator);
        relation.setRemark("由资源总览自动绑定");
        relation.setIsDeleted(false);
        relationMapper.insert(relation);
    }

    /** 计算当前资源已绑定的有效供应商数量。 */
    private Long boundSupplierCount(PurchaseResourceEntity entity) {
        return relationMapper.selectCount(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", entity.getTenantId())
                .eq("is_deleted", false)
                .eq("resource_type", entity.getResourceType())
                .eq("resource_id", entity.getId()));
    }

    /**
     * 批量统计当前页资源的供应商绑定数量。
     *
     * <p>资源总览页经常按 200 条拉取下拉数据，如果逐行执行 {@code count(*)}，远程数据库会产生
     * 200 次额外往返，弹窗打开会明显变慢。这里一次查出当前页所有采购关系，再在内存中按资源 ID 计数。</p>
     */
    private Map<Long, Long> boundSupplierCountMap(Long tenantId, List<PurchaseResourceEntity> records) {
        List<Long> resourceIds = records.stream()
                .map(PurchaseResourceEntity::getId)
                .filter(id -> id != null)
                .toList();
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        return relationMapper.selectList(new QueryWrapper<PurchaseRelationEntity>()
                        .select("resource_id")
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("resource_id", resourceIds))
                .stream()
                .collect(Collectors.groupingBy(PurchaseRelationEntity::getResourceId, Collectors.counting()));
    }

    /** 查询供应商名称，绑定列表中供应商被删除时仍保留空值，避免查询失败。 */
    private String supplierName(Long tenantId, Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        SupplierEntity supplier = supplierMapper.selectOne(new QueryWrapper<SupplierEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", supplierId));
        return supplier == null ? null : supplier.getSupplierName();
    }

    @Override
    protected PurchaseResourceEntity newEntity() {
        return new PurchaseResourceEntity();
    }

    @Override
    protected PurchaseResourceResponse toResponse(PurchaseResourceEntity entity) {
        return PurchaseResourceResponse.fromEntity(entity, boundSupplierCount(entity));
    }

    @Override
    protected String notFoundMessage() {
        return "采购资源不存在或已删除";
    }
}
