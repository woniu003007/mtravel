package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import com.mtravel.platform.purchase.relation.optional.mapper.PurchaseRelationOptionalItemMapper;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerOptionalItemsSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceOptionalItemEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceOptionalItemMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 保存产品中的自费项目最终对外价和素材快照，前端成本字段不作为可信来源。 */
@Service
public class SalesProductDesignerOptionalItemService {
    private static final String STATUS_ACTIVE = "active";

    private final SalesProductDayResourceMapper dayResourceMapper;
    private final SalesProductDayResourceOptionalItemMapper mapper;
    private final PurchaseResourceOptionalItemMapper masterMapper;
    private final PurchaseRelationOptionalItemMapper supplierMapper;
    private final PurchaseRelationMapper relationMapper;
    private final PurchaseResourceIntroductionMapper introductionMapper;

    public SalesProductDesignerOptionalItemService(
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductDayResourceOptionalItemMapper mapper,
            PurchaseResourceOptionalItemMapper masterMapper,
            PurchaseRelationOptionalItemMapper supplierMapper,
            PurchaseRelationMapper relationMapper,
            PurchaseResourceIntroductionMapper introductionMapper
    ) {
        this.dayResourceMapper = dayResourceMapper;
        this.mapper = mapper;
        this.masterMapper = masterMapper;
        this.supplierMapper = supplierMapper;
        this.relationMapper = relationMapper;
        this.introductionMapper = introductionMapper;
    }

    /**
     * 覆盖保存某日资源勾选的自费项目。前端的成本和建议价仅用于展示，始终重新读取供应商报价。
     */
    @Transactional
    public List<ProductDesignerSelectedOptionalItemResponse> save(
            Long tenantId,
            ProductDesignerOptionalItemsSaveRequest request,
            String operator
    ) {
        SalesProductDayResourceEntity dayResource = loadDayResource(tenantId, request);
        softDeleteForDayResource(tenantId, request.productId(), request.dayResourceId(), operator, OffsetDateTime.now());

        return saveRows(tenantId, request, operator,
                Objects.requireNonNullElse(request.selectedOptionalItems(), List.of()), null, dayResource);
    }

    /**
     * 供统一素材契约使用：自费项目仍写入原快照表，但排序采用普通介绍、自费项目混排后的全局顺序。
     */
    @Transactional
    public List<ProductDesignerSelectedOptionalItemResponse> saveWithGlobalSortOrders(
            Long tenantId,
            ProductDesignerOptionalItemsSaveRequest request,
            String operator,
            List<ProductDesignerSelectedOptionalItemRequest> rows,
            List<Integer> globalSortOrders
    ) {
        if (rows.size() != globalSortOrders.size()) {
            throw new BizException("自费项目排序数据不完整");
        }
        SalesProductDayResourceEntity dayResource = loadDayResource(tenantId, request);
        softDeleteForDayResource(tenantId, request.productId(), request.dayResourceId(), operator, OffsetDateTime.now());
        return saveRows(tenantId, request, operator, rows, globalSortOrders, dayResource);
    }

    private List<ProductDesignerSelectedOptionalItemResponse> saveRows(
            Long tenantId,
            ProductDesignerOptionalItemsSaveRequest request,
            String operator,
            List<ProductDesignerSelectedOptionalItemRequest> rows,
            List<Integer> globalSortOrders,
            SalesProductDayResourceEntity dayResource
    ) {
        for (int index = 0; index < rows.size(); index += 1) {
            ProductDesignerSelectedOptionalItemRequest row = rows.get(index);
            PurchaseResourceOptionalItemEntity master = loadActiveMaster(tenantId, dayResource, row);
            PurchaseRelationOptionalItemEntity supplier = loadSelectedSupplierQuote(tenantId, dayResource, master, row);
            if (supplier == null && row.salePrice() == null) {
                supplier = loadDefaultSupplierQuote(tenantId, dayResource, master);
            }
            java.math.BigDecimal finalSalePrice = row.salePrice() == null
                    ? defaultSalePrice(supplier)
                    : row.salePrice();
            PurchaseResourceIntroductionEntity introduction = loadSelectedIntroduction(tenantId, dayResource, master, row);

            SalesProductDayResourceOptionalItemEntity snapshot = new SalesProductDayResourceOptionalItemEntity();
            snapshot.setTenantId(tenantId);
            snapshot.setProductId(request.productId());
            snapshot.setDayResourceId(request.dayResourceId());
            snapshot.setResourceOptionalItemId(master.getId());
            snapshot.setSupplierOptionalItemId(supplier == null ? null : supplier.getId());
            snapshot.setItemTypeSnapshot(master.getItemType());
            snapshot.setProjectNameSnapshot(master.getProjectName());
            snapshot.setPriceUnitSnapshot("yuan_per_person");
            snapshot.setSupplierCostPriceSnapshot(supplier == null ? null : supplier.getCostPrice());
            snapshot.setSuggestedSalePriceSnapshot(supplier == null ? null : supplier.getSuggestedSalePrice());
            snapshot.setFinalSalePrice(finalSalePrice);
            snapshot.setSelectedIntroductionId(introduction == null ? null : introduction.getId());
            snapshot.setIntroductionTitleSnapshot(introduction == null ? null : introduction.getTitle());
            snapshot.setIntroductionContentSnapshot(introduction == null ? null : introduction.getContent());
            snapshot.setIntroductionNoticeSnapshot(introduction == null ? null : introduction.getNoticeContent());
            snapshot.setIntroductionWarmTipSnapshot(introduction == null ? null : introduction.getWarmTipContent());
            snapshot.setIntroductionVisitDurationSnapshot(introduction == null ? null : introduction.getVisitDuration());
            snapshot.setSortOrder(globalSortOrders == null ? index + 1 : globalSortOrders.get(index));
            snapshot.setCreatedBy(operator);
            snapshot.setIsDeleted(false);
            mapper.insert(snapshot);
        }
        return list(tenantId, request.productId(), request.dayResourceId());
    }

    /** 批量读取产品详情中每日资源的自费项目，避免按资源逐条查询。 */
    public Map<Long, List<ProductDesignerSelectedOptionalItemResponse>> listByDayResourceIds(
            Long tenantId,
            Long productId,
            List<Long> dayResourceIds
    ) {
        if (dayResourceIds == null || dayResourceIds.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new QueryWrapper<SalesProductDayResourceOptionalItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .in("day_resource_id", dayResourceIds)
                        .orderByAsc("day_resource_id")
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        SalesProductDayResourceOptionalItemEntity::getDayResourceId,
                        LinkedHashMap::new,
                        Collectors.mapping(ProductDesignerSelectedOptionalItemResponse::fromEntity, Collectors.toList())
                ));
    }

    /** 软删除一个每日资源的自费项目快照，需与主资源删除共用调用方事务。 */
    public void softDeleteForDayResource(
            Long tenantId,
            Long productId,
            Long dayResourceId,
            String operator,
            OffsetDateTime now
    ) {
        SalesProductDayResourceOptionalItemEntity deleted = new SalesProductDayResourceOptionalItemEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(now);
        deleted.setDeletedBy(operator);
        mapper.update(deleted, new UpdateWrapper<SalesProductDayResourceOptionalItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .eq("day_resource_id", dayResourceId)
                .eq("is_deleted", false));
    }

    /** 软删除一个产品草稿下所有自费项目快照，需与草稿删除共用调用方事务。 */
    public void softDeleteForProduct(Long tenantId, Long productId, String operator, OffsetDateTime now) {
        SalesProductDayResourceOptionalItemEntity deleted = new SalesProductDayResourceOptionalItemEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(now);
        deleted.setDeletedBy(operator);
        mapper.update(deleted, new UpdateWrapper<SalesProductDayResourceOptionalItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .eq("is_deleted", false));
    }

    public List<ProductDesignerSelectedOptionalItemResponse> list(Long tenantId, Long productId, Long dayResourceId) {
        return listByDayResourceIds(tenantId, productId, List.of(dayResourceId))
                .getOrDefault(dayResourceId, List.of());
    }

    private SalesProductDayResourceEntity loadDayResource(
            Long tenantId,
            ProductDesignerOptionalItemsSaveRequest request
    ) {
        SalesProductDayResourceEntity dayResource = dayResourceMapper.selectOne(
                new QueryWrapper<SalesProductDayResourceEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", request.productId())
                        .eq("id", request.dayResourceId())
                        .eq("is_deleted", false)
                        .last("limit 1")
        );
        if (dayResource == null) {
            throw new BizException("产品每日资源不存在或已删除");
        }
        return dayResource;
    }

    private PurchaseResourceOptionalItemEntity loadActiveMaster(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            ProductDesignerSelectedOptionalItemRequest row
    ) {
        PurchaseResourceOptionalItemEntity master = masterMapper.selectOne(
                new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("resource_id", dayResource.getResourceId())
                        .eq("id", row.resourceOptionalItemId())
                        .eq("is_deleted", false)
                        .eq("status", STATUS_ACTIVE)
                        .last("limit 1")
        );
        if (master == null) {
            throw new BizException("自费项目不属于当前景区或已停用");
        }
        return master;
    }

    private PurchaseRelationOptionalItemEntity loadSelectedSupplierQuote(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceOptionalItemEntity master,
            ProductDesignerSelectedOptionalItemRequest row
    ) {
        if (row.supplierOptionalItemId() == null) {
            return null;
        }
        if (dayResource.getSupplierId() == null) {
            throw new BizException("当前每日资源未选择供应商，不能选择供应商自费报价");
        }
        PurchaseRelationOptionalItemEntity supplier = supplierMapper.selectOne(
                new QueryWrapper<PurchaseRelationOptionalItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("id", row.supplierOptionalItemId())
                        .eq("resource_optional_item_id", master.getId())
                        .eq("is_deleted", false)
                        .eq("status", STATUS_ACTIVE)
                        .last("limit 1")
        );
        if (supplier == null || !matchesSelectedSupplierRelation(tenantId, dayResource, supplier)) {
            throw new BizException("供应商自费报价不属于当前资源、供应商或项目");
        }
        return supplier;
    }

    /** 对外价未填写时，只能使用当前每日资源所选供应商维护的建议价。 */
    private PurchaseRelationOptionalItemEntity loadDefaultSupplierQuote(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceOptionalItemEntity master
    ) {
        if (dayResource.getSupplierId() == null) {
            throw new BizException("未填写对外价，且当前资源未选择供应商报价");
        }
        List<PurchaseRelationOptionalItemEntity> candidates = supplierMapper.selectList(
                new QueryWrapper<PurchaseRelationOptionalItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("resource_optional_item_id", master.getId())
                        .eq("is_deleted", false)
                        .eq("status", STATUS_ACTIVE)
                        .orderByAsc("id"));
        return candidates.stream()
                .filter(candidate -> matchesSelectedSupplierRelation(tenantId, dayResource, candidate))
                .findFirst()
                .orElseThrow(() -> new BizException("当前供应商未维护该自费项目建议价，请填写对外价"));
    }

    private java.math.BigDecimal defaultSalePrice(PurchaseRelationOptionalItemEntity supplier) {
        if (supplier == null || supplier.getSuggestedSalePrice() == null) {
            throw new BizException("当前供应商未维护该自费项目建议价，请填写对外价");
        }
        return supplier.getSuggestedSalePrice();
    }

    private boolean matchesSelectedSupplierRelation(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseRelationOptionalItemEntity supplierQuote
    ) {
        PurchaseRelationEntity relation = relationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", supplierQuote.getRelationId())
                .eq("resource_id", dayResource.getResourceId())
                .eq("supplier_id", dayResource.getSupplierId())
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .last("limit 1"));
        return relation != null;
    }

    private PurchaseResourceIntroductionEntity loadSelectedIntroduction(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceOptionalItemEntity master,
            ProductDesignerSelectedOptionalItemRequest row
    ) {
        if (row.introductionId() == null) {
            return null;
        }
        PurchaseResourceIntroductionEntity introduction = introductionMapper.selectOne(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("resource_id", dayResource.getResourceId())
                        .eq("id", row.introductionId())
                        .eq("resource_optional_item_id", master.getId())
                        .eq("is_optional_item", true)
                        .eq("is_deleted", false)
                        .eq("status", "published")
                        .last("limit 1")
        );
        if (introduction == null) {
            throw new BizException("介绍素材不属于当前自费项目或未发布");
        }
        return introduction;
    }
}
