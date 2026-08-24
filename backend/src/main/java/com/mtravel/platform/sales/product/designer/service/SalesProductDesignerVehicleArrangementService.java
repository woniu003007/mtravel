package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementDeleteRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementReorderRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleResourceResponse;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDesignerVehicleArrangementEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDesignerVehicleArrangementMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理产品设计草稿的产品级全程用车快照。
 *
 * <p>全程用车只属于产品，不属于某一天，也不会流入地图或每日资源列表。所有可采购车辆的报价均由
 * {@link ProductDesignerSupplierQuoteService} 解析后冻结。</p>
 */
@Service
public class SalesProductDesignerVehicleArrangementService {

    private static final String PRODUCT_SCOPE_DESIGN_DRAFT = "design_draft";
    private static final String RESOURCE_TYPE_VEHICLE = "vehicle";
    private static final String STATUS_ACTIVE = "active";

    private final SalesProductMapper productMapper;
    private final PurchaseResourceMapper resourceMapper;
    private final SalesProductDesignerVehicleArrangementMapper vehicleMapper;
    private final ProductDesignerSupplierQuoteService supplierQuoteService;

    public SalesProductDesignerVehicleArrangementService(
            SalesProductMapper productMapper,
            PurchaseResourceMapper resourceMapper,
            SalesProductDesignerVehicleArrangementMapper vehicleMapper,
            ProductDesignerSupplierQuoteService supplierQuoteService
    ) {
        this.productMapper = productMapper;
        this.resourceMapper = resourceMapper;
        this.vehicleMapper = vehicleMapper;
        this.supplierQuoteService = supplierQuoteService;
    }

    /** 分页返回启用的用车资源；刻意不返回经纬度，避免进入每日地图候选。 */
    public PageResult<ProductDesignerVehicleResourceResponse> vehicleResources(
            Long tenantId,
            String keyword,
            long page,
            long pageSize
    ) {
        String cleanKeyword = clean(keyword);
        if (StringUtils.hasText(cleanKeyword) && cleanKeyword.length() < 2) {
            throw new BizException("资源关键词至少输入2个字符");
        }
        QueryWrapper<PurchaseResourceEntity> query = new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .eq("resource_type", RESOURCE_TYPE_VEHICLE)
                .like(StringUtils.hasText(cleanKeyword), "resource_name", cleanKeyword)
                .orderByAsc("seat_count")
                .orderByAsc("resource_name")
                .orderByAsc("id");
        Page<PurchaseResourceEntity> result = resourceMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 200)), query);
        return new PageResult<>(
                result.getRecords().stream().map(ProductDesignerVehicleResourceResponse::fromEntity).toList(),
                result.getTotal()
        );
    }

    /** 保存一条产品级用车，并冻结所选资源、供应商和成本。 */
    @Transactional
    public ProductDesignerVehicleArrangementResponse save(
            Long tenantId,
            ProductDesignerVehicleArrangementSaveRequest request,
            String operator
    ) {
        SalesProductEntity product = loadDraftProduct(tenantId, request.productId());
        validateDayRange(product, request.startDayNo(), request.endDayNo());
        SalesProductDesignerVehicleArrangementEntity entity = request.id() == null
                ? new SalesProductDesignerVehicleArrangementEntity()
                : loadVehicleArrangement(tenantId, request.productId(), request.id());

        PurchaseResourceEntity resource = request.resourceId() == null
                ? null
                : loadActiveVehicleResource(tenantId, request.resourceId());
        ProductDesignerSupplierQuote quote = resolveQuote(tenantId, resource, request);
        String vehicleType = firstText(clean(request.vehicleType()), resource == null ? null : clean(resource.getVehicleType()));
        if (!StringUtils.hasText(vehicleType)) {
            throw new BizException("请选择或填写车型");
        }
        BigDecimal quantity = request.quantity() != null
                ? request.quantity()
                : Objects.requireNonNullElse(entity.getQuantitySnapshot(), BigDecimal.ONE);
        if (quantity.signum() < 0) {
            throw new BizException("数量不能小于0");
        }
        quantity = quantity.setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitPrice = money(quote.unitPrice());
        BigDecimal costAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

        entity.setTenantId(tenantId);
        entity.setProductId(request.productId());
        entity.setResourceId(resource == null ? null : resource.getId());
        entity.setResourceNameSnapshot(resource == null ? vehicleType : clean(resource.getResourceName()));
        entity.setSupplierRelationIdSnapshot(quote.supplierRelationId());
        entity.setSupplierId(quote.supplierId());
        entity.setSupplierNameSnapshot(quote.supplierName());
        entity.setPriceModeSnapshot(quote.priceMode());
        entity.setVehicleTypeSnapshot(vehicleType);
        entity.setStartDayNo(request.startDayNo());
        entity.setEndDayNo(request.endDayNo());
        entity.setQuantitySnapshot(quantity);
        entity.setUnitPriceSnapshot(unitPrice);
        entity.setCostAmountSnapshot(costAmount);
        entity.setSortOrder(resolveSortOrder(tenantId, request.productId(), entity, request.sortOrder()));
        entity.setRemark(clean(request.remark()));

        if (request.id() == null) {
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            vehicleMapper.insert(entity);
        } else {
            int updated = vehicleMapper.update(entity, baseVehicleUpdate(tenantId, request.productId()).eq("id", request.id()));
            if (updated == 0) {
                throw new BizException("产品全程用车不存在或已删除");
            }
        }
        return ProductDesignerVehicleArrangementResponse.fromEntity(entity);
    }

    /** 软删除产品级的一条用车安排。 */
    @Transactional
    public void delete(
            Long tenantId,
            ProductDesignerVehicleArrangementDeleteRequest request,
            String operator
    ) {
        loadDraftProduct(tenantId, request.productId());
        loadVehicleArrangement(tenantId, request.productId(), request.vehicleArrangementId());
        SalesProductDesignerVehicleArrangementEntity deleted = new SalesProductDesignerVehicleArrangementEntity();
        markDeleted(deleted, operator, OffsetDateTime.now());
        int updated = vehicleMapper.update(deleted, baseVehicleUpdate(tenantId, request.productId())
                .eq("id", request.vehicleArrangementId()));
        if (updated == 0) {
            throw new BizException("产品全程用车不存在或已删除");
        }
    }

    /** 以完整 ID 列表保存产品级用车顺序，避免跨产品 ID 混入。 */
    @Transactional
    public void reorder(Long tenantId, ProductDesignerVehicleArrangementReorderRequest request) {
        loadDraftProduct(tenantId, request.productId());
        List<Long> requestedIds = request.vehicleArrangementIds() == null
                ? List.of()
                : request.vehicleArrangementIds();
        if (requestedIds.isEmpty()) {
            throw new BizException("用车排序不能为空");
        }
        if (requestedIds.stream().anyMatch(Objects::isNull)
                || requestedIds.stream().distinct().count() != requestedIds.size()) {
            throw new BizException("用车排序存在重复或空ID");
        }
        List<SalesProductDesignerVehicleArrangementEntity> current = activeVehicleEntities(tenantId, request.productId());
        Map<Long, SalesProductDesignerVehicleArrangementEntity> currentById = new LinkedHashMap<>();
        for (SalesProductDesignerVehicleArrangementEntity item : current) {
            currentById.put(item.getId(), item);
        }
        if (requestedIds.stream().anyMatch(id -> !currentById.containsKey(id))) {
            throw new BizException("用车排序中存在不属于当前产品的记录");
        }
        if (requestedIds.size() != currentById.size()) {
            throw new BizException("用车排序数据不完整");
        }
        for (int index = 0; index < requestedIds.size(); index += 1) {
            SalesProductDesignerVehicleArrangementEntity update = new SalesProductDesignerVehicleArrangementEntity();
            update.setSortOrder(index + 1);
            vehicleMapper.update(update, baseVehicleUpdate(tenantId, request.productId()).eq("id", requestedIds.get(index)));
        }
    }

    /** 按产品级排序返回有效的全程用车快照。 */
    public List<ProductDesignerVehicleArrangementResponse> list(Long tenantId, Long productId) {
        return activeVehicleEntities(tenantId, productId).stream()
                .sorted(Comparator.comparing(SalesProductDesignerVehicleArrangementEntity::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SalesProductDesignerVehicleArrangementEntity::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ProductDesignerVehicleArrangementResponse::fromEntity)
                .toList();
    }

    /** 汇总有效用车快照成本；供产品详情、成人报价和草稿删除主流程调用。 */
    public BigDecimal costAmount(Long tenantId, Long productId) {
        return activeVehicleEntities(tenantId, productId).stream()
                .map(SalesProductDesignerVehicleArrangementEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 正式产品或草稿被删除时，按产品边界批量软删用车快照。 */
    public void softDeleteForProduct(Long tenantId, Long productId, String operator) {
        SalesProductDesignerVehicleArrangementEntity deleted = new SalesProductDesignerVehicleArrangementEntity();
        markDeleted(deleted, operator, OffsetDateTime.now());
        vehicleMapper.update(deleted, baseVehicleUpdate(tenantId, productId));
    }

    private ProductDesignerSupplierQuote resolveQuote(
            Long tenantId,
            PurchaseResourceEntity resource,
            ProductDesignerVehicleArrangementSaveRequest request
    ) {
        if (resource == null) {
            if (request.supplierRelationId() != null) {
                throw new BizException("未选择用车资源时不能指定供应商采购关系");
            }
            return ProductDesignerSupplierQuote.pendingQuote();
        }
        return supplierQuoteService.resolve(tenantId, resource, request.supplierRelationId(), null);
    }

    private SalesProductEntity loadDraftProduct(Long tenantId, Long productId) {
        SalesProductEntity product = productMapper.selectOne(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_scope", PRODUCT_SCOPE_DESIGN_DRAFT)
                .eq("id", productId));
        if (product == null) {
            throw new BizException("产品设计草稿不存在或已完成设计");
        }
        return product;
    }

    private PurchaseResourceEntity loadActiveVehicleResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .eq("id", resourceId));
        if (resource == null) {
            throw new BizException("用车资源不存在、已停用或已删除");
        }
        if (!RESOURCE_TYPE_VEHICLE.equals(resource.getResourceType())) {
            throw new BizException("只能选择用车资源作为全程用车");
        }
        return resource;
    }

    private SalesProductDesignerVehicleArrangementEntity loadVehicleArrangement(
            Long tenantId,
            Long productId,
            Long vehicleArrangementId
    ) {
        SalesProductDesignerVehicleArrangementEntity entity = vehicleMapper.selectOne(
                baseVehicleQuery(tenantId, productId).eq("id", vehicleArrangementId));
        if (entity == null) {
            throw new BizException("产品全程用车不存在或已删除");
        }
        return entity;
    }

    private void validateDayRange(SalesProductEntity product, Integer startDayNo, Integer endDayNo) {
        if (startDayNo == null && endDayNo == null) {
            return;
        }
        if (startDayNo == null || endDayNo == null || startDayNo < 1 || endDayNo < startDayNo) {
            throw new BizException("用车起止天数不合法");
        }
        int travelDays = product.getTravelDays() == null ? 1 : product.getTravelDays();
        if (endDayNo > travelDays) {
            throw new BizException("用车结束天数不能超过产品行程天数");
        }
    }

    private int resolveSortOrder(
            Long tenantId,
            Long productId,
            SalesProductDesignerVehicleArrangementEntity entity,
            Integer requestedSortOrder
    ) {
        if (requestedSortOrder != null) {
            if (requestedSortOrder < 1) {
                throw new BizException("用车排序必须从1开始");
            }
            return requestedSortOrder;
        }
        if (entity.getId() != null && entity.getSortOrder() != null) {
            return entity.getSortOrder();
        }
        List<SalesProductDesignerVehicleArrangementEntity> current = activeVehicleEntities(tenantId, productId);
        return current.stream()
                .map(SalesProductDesignerVehicleArrangementEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private List<SalesProductDesignerVehicleArrangementEntity> activeVehicleEntities(Long tenantId, Long productId) {
        List<SalesProductDesignerVehicleArrangementEntity> entities = vehicleMapper.selectList(
                baseVehicleQuery(tenantId, productId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"));
        return entities == null ? List.of() : new ArrayList<>(entities);
    }

    private QueryWrapper<SalesProductDesignerVehicleArrangementEntity> baseVehicleQuery(Long tenantId, Long productId) {
        return new QueryWrapper<SalesProductDesignerVehicleArrangementEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId);
    }

    private UpdateWrapper<SalesProductDesignerVehicleArrangementEntity> baseVehicleUpdate(Long tenantId, Long productId) {
        return new UpdateWrapper<SalesProductDesignerVehicleArrangementEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId);
    }

    private void markDeleted(
            SalesProductDesignerVehicleArrangementEntity entity,
            String operator,
            OffsetDateTime now
    ) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(now);
        entity.setDeletedBy(operator);
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
