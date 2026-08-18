package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSupplierPriceRow;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceProcurementMode;
import com.mtravel.platform.purchase.resource.enums.ResourceStarLevel;
import com.mtravel.platform.purchase.resource.enums.ScenicLevel;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayPlanResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceDeleteRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceReorderRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDetailResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerIntroductionResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerIntroductionSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerMapResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerResourceDetailResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerResourceImageResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSupplierPriceLineResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSupplierResponse;
import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceImageEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 销售产品地图式设计工作台服务。
 *
 * <p>工作台按单条资源动作保存，避免复用产品模板完整保存接口而重建整组行程、团队安排和说明。
 * 成本、供应商、介绍发布状态和快照均由后端重新校验并落库。</p>
 */
@Service
public class SalesProductDesignerService {

    private static final String PRODUCT_SCOPE_TEMPLATE = "template";
    private static final String PRODUCT_SCOPE_DESIGN_DRAFT = "design_draft";
    private static final String STATUS_ACTIVE = "active";
    private static final String INTRODUCTION_PUBLISHED = "published";
    private static final String PRICE_MODE_UNIFIED = "unified";
    private static final String QUOTE_STATUS_DRAFT = "draft";
    private static final Set<String> LEVEL_RESOURCE_TYPES = Set.of("hotel", "restaurant");

    private final SalesProductMapper productMapper;
    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseRelationMapper relationMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierResourcePriceMapper priceMapper;
    private final PurchaseResourceIntroductionMapper introductionMapper;
    private final PurchaseResourceImageMapper imageMapper;
    private final SalesProductDayResourceMapper dayResourceMapper;
    private final SalesProductDayResourceImageMapper dayResourceImageMapper;
    private final SalesProductAdultQuoteMapper adultQuoteMapper;
    private final SalesProductDocumentVersionMapper documentVersionMapper;

    public SalesProductDesignerService(
            SalesProductMapper productMapper,
            PurchaseResourceMapper resourceMapper,
            PurchaseRelationMapper relationMapper,
            SupplierMapper supplierMapper,
            SupplierResourcePriceMapper priceMapper,
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceImageMapper imageMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductDayResourceImageMapper dayResourceImageMapper,
            SalesProductAdultQuoteMapper adultQuoteMapper,
            SalesProductDocumentVersionMapper documentVersionMapper
    ) {
        this.productMapper = productMapper;
        this.resourceMapper = resourceMapper;
        this.relationMapper = relationMapper;
        this.supplierMapper = supplierMapper;
        this.priceMapper = priceMapper;
        this.introductionMapper = introductionMapper;
        this.imageMapper = imageMapper;
        this.dayResourceMapper = dayResourceMapper;
        this.dayResourceImageMapper = dayResourceImageMapper;
        this.adultQuoteMapper = adultQuoteMapper;
        this.documentVersionMapper = documentVersionMapper;
    }

    /**
     * 分页查询产品设计草稿。
     *
     * <p>该查询只返回 design_draft，正式产品和团队快照不会出现在设计列表。</p>
     */
    public PageResult<ProductDesignerDraftResponse> pageDrafts(
            Long tenantId,
            String keyword,
            String businessType,
            String city,
            long page,
            long pageSize
    ) {
        String cleanKeyword = clean(keyword);
        Page<SalesProductEntity> result = productMapper.selectPage(
                Page.of(page, Math.min(pageSize, 200)),
                draftQuery(tenantId)
                        .eq(StringUtils.hasText(businessType), "business_type", clean(businessType))
                        .eq(StringUtils.hasText(city), "city", clean(city))
                        .and(StringUtils.hasText(cleanKeyword), nested -> nested
                                .like("product_name", cleanKeyword)
                                .or()
                                .like("city", cleanKeyword))
                        .orderByDesc("updated_at")
                        .orderByDesc("id")
        );
        return new PageResult<>(
                result.getRecords().stream().map(ProductDesignerDraftResponse::fromEntity).toList(),
                result.getTotal()
        );
    }

    /** 查询产品设计草稿基础信息。 */
    public ProductDesignerDraftResponse draftDetail(Long tenantId, Long draftId) {
        return ProductDesignerDraftResponse.fromEntity(loadProduct(tenantId, draftId));
    }

    /** 新建产品设计草稿，草稿不进入产品管理。 */
    @Transactional
    public ProductDesignerDraftResponse createDraft(
            Long tenantId,
            ProductDesignerDraftSaveRequest request,
            String operator
    ) {
        assertDraftNameAvailable(tenantId, request.productName(), null);
        SalesProductEntity entity = new SalesProductEntity();
        entity.setTenantId(tenantId);
        entity.setProductScope(PRODUCT_SCOPE_DESIGN_DRAFT);
        applyDraftFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        try {
            productMapper.insert(entity);
        } catch (DataIntegrityViolationException exception) {
            // 数据库部分唯一索引作为跨实例并发草稿/正式产品重名的最终保护。
            throw duplicateProductNameException();
        }
        return ProductDesignerDraftResponse.fromEntity(entity);
    }

    /** 修改产品设计草稿基础信息。 */
    @Transactional
    public ProductDesignerDraftResponse updateDraft(
            Long tenantId,
            Long draftId,
            ProductDesignerDraftSaveRequest request
    ) {
        loadProduct(tenantId, draftId);
        assertDraftNameAvailable(tenantId, request.productName(), draftId);
        int travelDays = request.travelDays() == null ? 1 : request.travelDays();
        Long overflowCount = dayResourceMapper.selectCount(new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", draftId)
                .gt("day_no", travelDays));
        if (overflowCount != null && overflowCount > 0) {
            throw new BizException("请先移除超出新行程天数的资源安排");
        }
        SalesProductEntity entity = new SalesProductEntity();
        applyDraftFields(entity, request);
        int updated;
        try {
            updated = productMapper.update(entity, new UpdateWrapper<SalesProductEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("is_deleted", false)
                    .eq("product_scope", PRODUCT_SCOPE_DESIGN_DRAFT)
                    .eq("id", draftId));
        } catch (DataIntegrityViolationException exception) {
            // 预查之后其它会话也可能占用名称，需转换为稳定的业务错误。
            throw duplicateProductNameException();
        }
        if (updated == 0) {
            throw new BizException("产品设计草稿不存在或已删除");
        }
        return draftDetail(tenantId, draftId);
    }

    /**
     * 完成产品设计并转为正式产品模板。
     *
     * <p>只有至少编排一项资源的草稿可以完成；转正与重名校验在同一事务内执行。</p>
     *
     * @return 转为正式产品后的产品ID
     */
    @Transactional
    public Long publishDraft(Long tenantId, Long draftId) {
        SalesProductEntity draft = loadProduct(tenantId, draftId);
        Long resourceCount = dayResourceMapper.selectCount(new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", draftId));
        if (resourceCount == null || resourceCount == 0) {
            throw new BizException("请先编排行程资源再完成设计");
        }
        Long formalProductCount = productMapper.selectCount(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_scope", PRODUCT_SCOPE_TEMPLATE)
                .eq("product_name", draft.getProductName())
                .ne("id", draftId));
        if (formalProductCount != null && formalProductCount > 0) {
            throw new BizException("产品管理中已存在同名产品，请先修改草稿名称");
        }
        SalesProductEntity published = new SalesProductEntity();
        published.setProductScope(PRODUCT_SCOPE_TEMPLATE);
        published.setStatus(STATUS_ACTIVE);
        int updated;
        try {
            updated = productMapper.update(published, new UpdateWrapper<SalesProductEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("is_deleted", false)
                    .eq("product_scope", PRODUCT_SCOPE_DESIGN_DRAFT)
                    .eq("id", draftId));
        } catch (DataIntegrityViolationException exception) {
            // 完成设计会将草稿纳入正式产品唯一名称范围，必须处理竞态冲突。
            throw duplicateProductNameException();
        }
        if (updated == 0) {
            throw new BizException("产品设计草稿状态已变更，请刷新后重试");
        }
        return draftId;
    }

    /**
     * 软删除一条产品设计草稿及其仅供设计工作台使用的子数据。
     *
     * <p>删除条件始终带租户、未删除和 design_draft 范围，正式产品不能借此接口删除。</p>
     */
    @Transactional
    public void deleteDraft(Long tenantId, Long draftId, String operator) {
        loadProduct(tenantId, draftId);
        OffsetDateTime now = OffsetDateTime.now();
        softDeleteDraftChildren(tenantId, draftId, operator, now);

        SalesProductEntity deleted = new SalesProductEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(now);
        deleted.setDeletedBy(operator);
        int updated = productMapper.update(deleted, new UpdateWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_scope", PRODUCT_SCOPE_DESIGN_DRAFT)
                .eq("id", draftId));
        if (updated == 0) {
            throw new BizException("产品设计草稿不存在或已删除");
        }
    }

    /** 查询产品设计工作台详情，按天返回已加入资源和成本摘要。 */
    public ProductDesignerDetailResponse detail(Long tenantId, Long productId) {
        SalesProductEntity product = loadProduct(tenantId, productId);
        List<SalesProductDayResourceEntity> resources = dayResourceMapper.selectList(new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId)
                .orderByAsc("day_no")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
        Map<Integer, List<SalesProductDayResourceEntity>> byDay = resources.stream()
                .collect(Collectors.groupingBy(
                        SalesProductDayResourceEntity::getDayNo,
                        LinkedHashMap::new,
                    Collectors.toList()
                ));
        Map<Long, List<Long>> selectedImageIds = selectedImageIds(tenantId, productId, resources);
        int daysCount = product.getTravelDays() == null ? 1 : product.getTravelDays();
        List<ProductDesignerDayPlanResponse> days = java.util.stream.IntStream.rangeClosed(1, daysCount)
                .mapToObj(dayNo -> dayPlan(dayNo, byDay.getOrDefault(dayNo, List.of()), selectedImageIds))
                .toList();
        BigDecimal totalCost = resources.stream()
                .map(SalesProductDayResourceEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProductDesignerDetailResponse(
                product.getId(),
                product.getProductName(),
                product.getProvince(),
                product.getCity(),
                daysCount,
                product.getStatus(),
                money(totalCost),
                days,
                ProductDesignerAdultQuoteResponse.fromEntity(currentAdultQuote(tenantId, productId))
        );
    }

    /** 分页查询资源池；有坐标的资源用于地图点位，无坐标资源仍可从列表加入行程。 */
    public PageResult<ProductDesignerMapResourceResponse> resources(
            Long tenantId,
            String keyword,
            String resourceType,
            String province,
            String city,
            String scenicLevel,
            String starLevel,
            long page,
            long pageSize
    ) {
        String cleanKeyword = clean(keyword);
        if (StringUtils.hasText(cleanKeyword) && cleanKeyword.length() < 2) {
            throw new BizException("资源关键词至少输入2个字符");
        }
        validateLevelFilters(resourceType, scenicLevel, starLevel);
        QueryWrapper<PurchaseResourceEntity> wrapper = new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .ne("resource_type", "traffic")
                .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                .eq(StringUtils.hasText(province), "province", clean(province))
                .in(StringUtils.hasText(city), "city", cityCandidates(city))
                .eq(StringUtils.hasText(scenicLevel), "scenic_level", clean(scenicLevel))
                .eq(StringUtils.hasText(starLevel), "star_level", clean(starLevel))
                .like(StringUtils.hasText(cleanKeyword), "resource_name", cleanKeyword)
                .orderByAsc("city")
                .orderByAsc("resource_type")
                .orderByAsc("resource_name");
        Page<PurchaseResourceEntity> result = resourceMapper.selectPage(Page.of(page, Math.min(pageSize, 200)), wrapper);
        Map<Long, ProductDesignerSupplierResponse> defaultSuppliers = defaultSupplierMap(tenantId, result.getRecords());
        List<ProductDesignerMapResourceResponse> items = result.getRecords().stream()
                .map(resource -> {
                    ProductDesignerSupplierResponse supplier = defaultSuppliers.get(resource.getId());
                    return new ProductDesignerMapResourceResponse(
                            resource.getId(),
                            resource.getResourceType(),
                            resource.getProcurementMode(),
                            resource.getResourceName(),
                            resource.getProvince(),
                            resource.getCity(),
                            resource.getDistrict(),
                            resource.getAddress(),
                            resource.getLongitude(),
                            resource.getLatitude(),
                            resource.getStatus(),
                            supplier == null ? null : supplier.relationId(),
                            supplier == null ? null : supplier.supplierId(),
                            supplier == null ? null : supplier.supplierName(),
                            supplier == null ? BigDecimal.ZERO : supplier.referenceUnitPrice()
                    );
                })
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 只允许景区使用 A 级筛选，酒店和餐厅使用星级筛选。 */
    private void validateLevelFilters(String resourceType, String scenicLevel, String starLevel) {
        if (StringUtils.hasText(scenicLevel)
                && (!"scenic".equals(resourceType) || !ScenicLevel.contains(scenicLevel))) {
            throw new BizException("景区等级筛选只能用于景区，且筛选值不合法");
        }
        if (StringUtils.hasText(starLevel)
                && (!LEVEL_RESOURCE_TYPES.contains(resourceType) || !ResourceStarLevel.contains(starLevel))) {
            throw new BizException("星级筛选只能用于酒店或餐厅，且筛选值不合法");
        }
    }

    /**
     * 城市筛选同时兼容“苏州”和“苏州市”两种历史保存值。
     *
     * <p>新页面会提交标准地级市名称，旧调用或历史数据仍可能只保存城市简称，
     * 因此这里使用两个候选值查询，不修改资源主档数据。</p>
     */
    private List<String> cityCandidates(String city) {
        String value = clean(city);
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        if (value.endsWith("市") && value.length() > 1) {
            return List.of(value, value.substring(0, value.length() - 1));
        }
        return List.of(value, value + "市");
    }

    /** 查询单个资源的介绍、图片和有效供应商绑定。 */
    public ProductDesignerResourceDetailResponse resourceDetail(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = loadActiveResource(tenantId, resourceId);
        List<PurchaseResourceIntroductionEntity> introductions = publishedIntroductions(tenantId, resourceId);
        List<PurchaseResourceImageEntity> images = imageMapper.selectList(new QueryWrapper<PurchaseResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", STATUS_ACTIVE)
                .orderByDesc("is_cover")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
        List<ProductDesignerSupplierResponse> suppliers = supplierResponses(tenantId, resource);
        Long defaultSupplierId = suppliers.stream()
                .filter(item -> Boolean.TRUE.equals(item.isDefault()))
                .map(ProductDesignerSupplierResponse::supplierId)
                .findFirst()
                .orElse(null);
        return new ProductDesignerResourceDetailResponse(
                resource.getId(),
                resource.getResourceType(),
                resource.getProcurementMode(),
                resource.getResourceName(),
                resource.getProvince(),
                resource.getCity(),
                resource.getDistrict(),
                resource.getAddress(),
                resource.getLongitude(),
                resource.getLatitude(),
                resource.getIntroduction(),
                resource.getWarmTip(),
                introductions.stream().map(ProductDesignerIntroductionResponse::fromEntity).toList(),
                images.stream().map(ProductDesignerResourceImageResponse::fromEntity).toList(),
                suppliers,
                defaultSupplierId
        );
    }

    /** 新增或修改某天的一条资源，并保存供应商成本和资源介绍快照。 */
    @Transactional
    public ProductDesignerDayResourceResponse saveDayResource(
            Long tenantId,
            ProductDesignerDayResourceSaveRequest request,
            String operator
    ) {
        SalesProductEntity product = loadProduct(tenantId, request.productId());
        validateDayNo(product, request.dayNo());
        PurchaseResourceEntity resource = loadActiveResource(tenantId, request.resourceId());
        assertNotDuplicateDayResource(tenantId, request);

        SalesProductDayResourceEntity entity = request.id() == null
                ? new SalesProductDayResourceEntity()
                : loadDayResource(tenantId, request.productId(), request.id());
        PurchaseRelationEntity relation = relationForSave(tenantId, resource, request.supplierId());
        PurchaseResourceIntroductionEntity introduction = changedIntroductionForSave(
                tenantId,
                resource.getId(),
                request.selectedIntroductionId(),
                entity
        );
        BigDecimal quantity = request.quantity() == null ? BigDecimal.ONE : request.quantity();
        BigDecimal unitPrice = unitPriceForSave(tenantId, resource, relation);
        BigDecimal costAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        entity.setTenantId(tenantId);
        entity.setProductId(request.productId());
        entity.setDayNo(request.dayNo());
        entity.setResourceId(resource.getId());
        entity.setResourceNameSnapshot(resource.getResourceName());
        entity.setResourceTypeSnapshot(resource.getResourceType());
        entity.setProvinceSnapshot(resource.getProvince());
        entity.setCitySnapshot(resource.getCity());
        entity.setDistrictSnapshot(resource.getDistrict());
        entity.setAddressSnapshot(resource.getAddress());
        entity.setLongitudeSnapshot(resource.getLongitude());
        entity.setLatitudeSnapshot(resource.getLatitude());
        entity.setProcurementModeSnapshot(resource.getProcurementMode());
        int fallbackSortOrder = entity.getSortOrder() == null
                ? nextSortOrder(tenantId, request.productId(), request.dayNo())
                : entity.getSortOrder();
        entity.setSortOrder(request.sortOrder() == null ? fallbackSortOrder : request.sortOrder());
        entity.setStayMinutes(request.stayMinutes() == null ? 0 : request.stayMinutes());
        entity.setIncludeInWord(request.includeInWord() == null || request.includeInWord());
        entity.setSupplierId(relation == null ? null : relation.getSupplierId());
        entity.setSupplierNameSnapshot(relation == null ? null : supplierName(tenantId, relation.getSupplierId()));
        entity.setUnitPriceSnapshot(unitPrice);
        entity.setQuantitySnapshot(quantity);
        entity.setCostAmountSnapshot(costAmount);
        applyIntroductionSnapshotForSave(entity, resource.getId(), request.selectedIntroductionId(), introduction);
        entity.setRemark(clean(request.remark()));
        if (request.id() == null) {
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            dayResourceMapper.insert(entity);
        } else {
            int updated = dayResourceMapper.update(entity, baseDayResourceUpdate(tenantId, request.productId()).eq("id", request.id()));
            if (updated == 0) {
                throw new BizException("产品每日资源不存在或已删除");
            }
        }
        saveSelectedImages(tenantId, entity, resource, request.selectedImageIds(), operator);
        return ProductDesignerDayResourceResponse.fromEntity(entity, selectedImageIdsFor(tenantId, entity.getId()));
    }

    /** 软删除产品某天的一条资源。 */
    @Transactional
    public void deleteDayResource(Long tenantId, ProductDesignerDayResourceDeleteRequest request, String operator) {
        UpdateWrapper<SalesProductDayResourceEntity> wrapper = baseDayResourceUpdate(tenantId, request.productId())
                .eq("id", request.id())
                .set("is_deleted", true)
                .set("deleted_at", OffsetDateTime.now())
                .set("deleted_by", operator);
        int updated = dayResourceMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BizException("产品每日资源不存在或已删除");
        }
    }

    /** 保存当前天资源顺序。 */
    @Transactional
    public void reorderDayResources(Long tenantId, ProductDesignerDayResourceReorderRequest request) {
        loadProduct(tenantId, request.productId());
        List<SalesProductDayResourceEntity> current = dayResourceMapper.selectList(new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", request.productId())
                .eq("day_no", request.dayNo()));
        Map<Long, SalesProductDayResourceEntity> currentMap = current.stream()
                .collect(Collectors.toMap(SalesProductDayResourceEntity::getId, Function.identity()));
        if (!currentMap.keySet().containsAll(request.dayResourceIds())) {
            throw new BizException("排序资源不属于当前产品或已删除");
        }
        for (int index = 0; index < request.dayResourceIds().size(); index += 1) {
            SalesProductDayResourceEntity entity = new SalesProductDayResourceEntity();
            entity.setSortOrder(index + 1);
            dayResourceMapper.update(entity, baseDayResourceUpdate(tenantId, request.productId())
                    .eq("day_no", request.dayNo())
                    .eq("id", request.dayResourceIds().get(index)));
        }
    }

    /** 单独替换某条资源的介绍版本并更新正文快照。 */
    @Transactional
    public ProductDesignerDayResourceResponse saveIntroduction(Long tenantId, ProductDesignerIntroductionSaveRequest request) {
        SalesProductDayResourceEntity entity = loadDayResource(tenantId, request.productId(), request.dayResourceId());
        PurchaseResourceIntroductionEntity introduction = introductionForSave(
                tenantId,
                entity.getResourceId(),
                request.selectedIntroductionId()
        );
        applyIntroductionSnapshot(entity, introduction);
        dayResourceMapper.update(entity, baseDayResourceUpdate(tenantId, request.productId()).eq("id", request.dayResourceId()));
        return ProductDesignerDayResourceResponse.fromEntity(entity, selectedImageIdsFor(tenantId, entity.getId()));
    }

    /** 保存成人报价草稿；成人成本始终按当前已选资源后端快照重新计算。 */
    @Transactional
    public ProductDesignerAdultQuoteResponse saveAdultQuote(
            Long tenantId,
            ProductDesignerAdultQuoteSaveRequest request,
            String operator
    ) {
        loadProduct(tenantId, request.productId());
        SalesProductAdultQuoteEntity entity = request.id() == null
                ? new SalesProductAdultQuoteEntity()
                : loadAdultQuote(tenantId, request.productId(), request.id());
        BigDecimal totalCost = productCost(tenantId, request.productId());
        BigDecimal adultCount = BigDecimal.valueOf(request.plannedAdultCount());
        BigDecimal adultCost = totalCost.divide(adultCount, 2, RoundingMode.HALF_UP);
        BigDecimal markup = request.markupAmount() == null ? BigDecimal.ZERO : request.markupAmount();
        BigDecimal saleAmount = request.adultSaleAmount() == null
                ? adultCost.add(markup).setScale(2, RoundingMode.HALF_UP)
                : request.adultSaleAmount().setScale(2, RoundingMode.HALF_UP);
        if (request.adultSaleAmount() != null
                && request.markupAmount() != null
                && saleAmount.compareTo(adultCost.add(markup).setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BizException("成人对外价必须等于后端成本加人工加价");
        }
        if (request.adultSaleAmount() != null && request.markupAmount() == null) {
            markup = saleAmount.subtract(adultCost).setScale(2, RoundingMode.HALF_UP);
            if (markup.signum() < 0) {
                throw new BizException("成人对外价不能低于后端成本");
            }
        }
        entity.setTenantId(tenantId);
        entity.setProductId(request.productId());
        entity.setPlannedAdultCount(request.plannedAdultCount());
        entity.setAdultCostAmount(adultCost);
        entity.setMarkupAmount(markup.setScale(2, RoundingMode.HALF_UP));
        entity.setAdultSaleAmount(saleAmount);
        entity.setValidUntil(request.validUntil());
        entity.setQuoteRemark(clean(request.quoteRemark()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : QUOTE_STATUS_DRAFT);
        if (request.id() == null) {
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            adultQuoteMapper.insert(entity);
        } else {
            int updated = adultQuoteMapper.update(entity, new UpdateWrapper<SalesProductAdultQuoteEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("is_deleted", false)
                    .eq("product_id", request.productId())
                    .eq("id", request.id()));
            if (updated == 0) {
                throw new BizException("成人报价草稿不存在或已删除");
            }
        }
        return ProductDesignerAdultQuoteResponse.fromEntity(entity);
    }

    private ProductDesignerDayPlanResponse dayPlan(
            Integer dayNo,
            List<SalesProductDayResourceEntity> resources,
            Map<Long, List<Long>> selectedImageIds
    ) {
        BigDecimal dayCost = resources.stream()
                .map(SalesProductDayResourceEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProductDesignerDayPlanResponse(
                dayNo,
                money(dayCost),
                resources.stream()
                        .map(item -> ProductDesignerDayResourceResponse.fromEntity(
                                item, selectedImageIds.getOrDefault(item.getId(), List.of())
                        ))
                        .toList()
        );
    }

    private Map<Long, List<Long>> selectedImageIds(
            Long tenantId,
            Long productId,
            List<SalesProductDayResourceEntity> resources
    ) {
        List<Long> dayResourceIds = resources.stream()
                .map(SalesProductDayResourceEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (dayResourceIds.isEmpty()) {
            return Map.of();
        }
        return dayResourceImageMapper.selectList(new QueryWrapper<SalesProductDayResourceImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .in("day_resource_id", dayResourceIds)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        SalesProductDayResourceImageEntity::getDayResourceId,
                        LinkedHashMap::new,
                        Collectors.mapping(SalesProductDayResourceImageEntity::getResourceImageId, Collectors.toList())
                ));
    }

    private List<Long> selectedImageIdsFor(Long tenantId, Long dayResourceId) {
        if (dayResourceId == null) {
            return List.of();
        }
        return dayResourceImageMapper.selectList(new QueryWrapper<SalesProductDayResourceImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("day_resource_id", dayResourceId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(SalesProductDayResourceImageEntity::getResourceImageId)
                .toList();
    }

    private void saveSelectedImages(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceEntity resource,
            List<Long> selectedImageIds,
            String operator
    ) {
        if (selectedImageIds == null || dayResource.getId() == null) {
            return;
        }
        List<Long> imageIds = selectedImageIds.stream().filter(Objects::nonNull).distinct().toList();
        List<PurchaseResourceImageEntity> images = imageIds.isEmpty()
                ? List.of()
                : imageMapper.selectList(new QueryWrapper<PurchaseResourceImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resource.getId())
                        .eq("status", STATUS_ACTIVE)
                        .in("id", imageIds));
        if (images.size() != imageIds.size()) {
            throw new BizException("所选图片不属于当前资源或已停用");
        }
        SalesProductDayResourceImageEntity deleted = new SalesProductDayResourceImageEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(OffsetDateTime.now());
        deleted.setDeletedBy(operator);
        dayResourceImageMapper.update(deleted, new UpdateWrapper<SalesProductDayResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", dayResource.getProductId())
                .eq("day_resource_id", dayResource.getId()));
        Map<Long, PurchaseResourceImageEntity> imageById = images.stream()
                .collect(Collectors.toMap(PurchaseResourceImageEntity::getId, Function.identity()));
        for (int index = 0; index < imageIds.size(); index += 1) {
            PurchaseResourceImageEntity image = imageById.get(imageIds.get(index));
            SalesProductDayResourceImageEntity selected = new SalesProductDayResourceImageEntity();
            selected.setTenantId(tenantId);
            selected.setProductId(dayResource.getProductId());
            selected.setDayResourceId(dayResource.getId());
            selected.setResourceImageId(image.getId());
            selected.setAttachmentId(image.getAttachmentId());
            selected.setOriginalFilenameSnapshot(image.getOriginalFilename());
            selected.setSortOrder(index + 1);
            selected.setCreatedBy(operator);
            selected.setIsDeleted(false);
            dayResourceImageMapper.insert(selected);
        }
    }

    private SalesProductEntity loadProduct(Long tenantId, Long productId) {
        SalesProductEntity product = productMapper.selectOne(draftQuery(tenantId).eq("id", productId));
        if (product == null) {
            throw new BizException("产品设计草稿不存在或已完成设计");
        }
        return product;
    }

    private QueryWrapper<SalesProductEntity> draftQuery(Long tenantId) {
        return new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_scope", PRODUCT_SCOPE_DESIGN_DRAFT);
    }

    private void applyDraftFields(SalesProductEntity entity, ProductDesignerDraftSaveRequest request) {
        entity.setProductName(clean(request.productName()));
        entity.setBusinessType(clean(request.businessType()));
        entity.setDomesticInternational(
                "international".equals(request.domesticInternational()) ? "international" : "domestic"
        );
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setTripType("irregular");
        entity.setReceptionStandard(clean(request.receptionStandard()));
        entity.setProductTheme(clean(request.productTheme()));
        entity.setTravelDays(request.travelDays() == null ? 1 : request.travelDays());
        entity.setCloseDaysBefore(0);
        entity.setSingleRoomDifference(BigDecimal.ZERO);
        entity.setPlannedCapacity(0);
        entity.setStatus(STATUS_ACTIVE);
        entity.setRemark(clean(request.remark()));
    }

    private void assertDraftNameAvailable(Long tenantId, String productName, Long excludeId) {
        Long count = productMapper.selectCount(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("product_scope", List.of(PRODUCT_SCOPE_DESIGN_DRAFT, PRODUCT_SCOPE_TEMPLATE))
                .eq("product_name", clean(productName))
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw duplicateProductNameException();
        }
    }

    /** 返回预查和数据库唯一约束共享的重名提示。 */
    private BizException duplicateProductNameException() {
        return new BizException("同名产品或设计草稿已存在");
    }

    /** 软删除草稿私有子表，避免已删除草稿的成本、图片和生成文件继续被业务查询到。 */
    private void softDeleteDraftChildren(Long tenantId, Long draftId, String operator, OffsetDateTime now) {
        SalesProductDayResourceImageEntity deletedImage = new SalesProductDayResourceImageEntity();
        markDeleted(deletedImage, operator, now);
        dayResourceImageMapper.update(deletedImage, new UpdateWrapper<SalesProductDayResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", draftId));

        SalesProductDayResourceEntity deletedResource = new SalesProductDayResourceEntity();
        markDeleted(deletedResource, operator, now);
        dayResourceMapper.update(deletedResource, new UpdateWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", draftId));

        SalesProductAdultQuoteEntity deletedQuote = new SalesProductAdultQuoteEntity();
        markDeleted(deletedQuote, operator, now);
        adultQuoteMapper.update(deletedQuote, new UpdateWrapper<SalesProductAdultQuoteEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", draftId));

        SalesProductDocumentVersionEntity deletedDocument = new SalesProductDocumentVersionEntity();
        markDeleted(deletedDocument, operator, now);
        documentVersionMapper.update(deletedDocument, new UpdateWrapper<SalesProductDocumentVersionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", draftId));
    }

    /** 填充统一软删除审计字段。 */
    private void markDeleted(com.mtravel.platform.common.TenantSoftDeleteEntity entity, String operator, OffsetDateTime now) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(now);
        entity.setDeletedBy(operator);
    }

    private PurchaseResourceEntity loadActiveResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .eq("id", resourceId));
        if (resource == null) {
            throw new BizException("采购资源不存在、已停用或已删除");
        }
        return resource;
    }

    private SalesProductDayResourceEntity loadDayResource(Long tenantId, Long productId, Long dayResourceId) {
        SalesProductDayResourceEntity entity = dayResourceMapper.selectOne(baseDayResourceQuery(tenantId, productId)
                .eq("id", dayResourceId));
        if (entity == null) {
            throw new BizException("产品每日资源不存在或已删除");
        }
        return entity;
    }

    private SalesProductAdultQuoteEntity loadAdultQuote(Long tenantId, Long productId, Long quoteId) {
        SalesProductAdultQuoteEntity entity = adultQuoteMapper.selectOne(new QueryWrapper<SalesProductAdultQuoteEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId)
                .eq("id", quoteId));
        if (entity == null) {
            throw new BizException("成人报价草稿不存在或已删除");
        }
        return entity;
    }

    private QueryWrapper<SalesProductDayResourceEntity> baseDayResourceQuery(Long tenantId, Long productId) {
        return new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId);
    }

    private UpdateWrapper<SalesProductDayResourceEntity> baseDayResourceUpdate(Long tenantId, Long productId) {
        return new UpdateWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId);
    }

    private void validateDayNo(SalesProductEntity product, Integer dayNo) {
        int travelDays = product.getTravelDays() == null ? 1 : product.getTravelDays();
        if (dayNo == null || dayNo < 1 || dayNo > travelDays) {
            throw new BizException("行程天数超出产品天数范围");
        }
    }

    private void assertNotDuplicateDayResource(Long tenantId, ProductDesignerDayResourceSaveRequest request) {
        Long count = dayResourceMapper.selectCount(baseDayResourceQuery(tenantId, request.productId())
                .eq("day_no", request.dayNo())
                .eq("resource_id", request.resourceId())
                .ne(request.id() != null, "id", request.id()));
        if (count != null && count > 0) {
            throw new BizException("同一天不能重复加入同一个资源");
        }
    }

    private PurchaseRelationEntity relationForSave(Long tenantId, PurchaseResourceEntity resource, Long supplierId) {
        if (PurchaseResourceProcurementMode.NOT_REQUIRED.value().equals(resource.getProcurementMode())) {
            return null;
        }
        QueryWrapper<PurchaseRelationEntity> wrapper = new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .eq("resource_type", resource.getResourceType())
                .eq("resource_id", resource.getId())
                .eq(supplierId != null, "supplier_id", supplierId)
                .orderByDesc("is_default")
                .orderByAsc("group_quantity")
                .orderByAsc("id")
                .last("limit 1");
        PurchaseRelationEntity relation = relationMapper.selectOne(wrapper);
        if (relation == null) {
            throw new BizException(supplierId == null ? "该资源没有有效供应商报价" : "供应商未绑定当前资源或已停用");
        }
        return relation;
    }

    private PurchaseResourceIntroductionEntity introductionForSave(Long tenantId, Long resourceId, Long introductionId) {
        if (introductionId == null) {
            return null;
        }
        PurchaseResourceIntroductionEntity introduction = introductionMapper.selectOne(new QueryWrapper<PurchaseResourceIntroductionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", INTRODUCTION_PUBLISHED)
                .eq("id", introductionId));
        if (introduction == null) {
            throw new BizException("只能选择当前资源已发布的介绍版本");
        }
        return introduction;
    }

    /**
     * 修改每日资源时，只有介绍版本真的变更了才重新校验已发布状态；纯编辑停留时间、备注等场景保留历史快照。
     */
    private PurchaseResourceIntroductionEntity changedIntroductionForSave(
            Long tenantId,
            Long resourceId,
            Long introductionId,
            SalesProductDayResourceEntity entity
    ) {
        if (Objects.equals(resourceId, entity.getResourceId())
                && Objects.equals(introductionId, entity.getSelectedIntroductionId())) {
            return null;
        }
        return introductionForSave(tenantId, resourceId, introductionId);
    }

    private List<PurchaseResourceIntroductionEntity> publishedIntroductions(Long tenantId, Long resourceId) {
        return introductionMapper.selectList(new QueryWrapper<PurchaseResourceIntroductionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", INTRODUCTION_PUBLISHED)
                .orderByDesc("published_at")
                .orderByDesc("id"));
    }

    private void applyIntroductionSnapshot(
            SalesProductDayResourceEntity entity,
            PurchaseResourceIntroductionEntity introduction
    ) {
        if (introduction == null) {
            entity.setSelectedIntroductionId(null);
            entity.setIntroductionIndexVersion(null);
            entity.setIntroductionTitleSnapshot(null);
            entity.setIntroductionContentSnapshot(null);
            entity.setIntroductionNoticeSnapshot(null);
            return;
        }
        entity.setSelectedIntroductionId(introduction.getId());
        entity.setIntroductionIndexVersion(introduction.getIndexVersion());
        entity.setIntroductionTitleSnapshot(introduction.getTitle());
        entity.setIntroductionContentSnapshot(introduction.getContent());
        entity.setIntroductionNoticeSnapshot(clean(introduction.getNoticeContent()));
    }

    private void applyIntroductionSnapshotForSave(
            SalesProductDayResourceEntity entity,
            Long resourceId,
            Long selectedIntroductionId,
            PurchaseResourceIntroductionEntity introduction
    ) {
        if (Objects.equals(resourceId, entity.getResourceId())
                && Objects.equals(selectedIntroductionId, entity.getSelectedIntroductionId())) {
            return;
        }
        applyIntroductionSnapshot(entity, introduction);
    }

    private BigDecimal unitPriceForSave(Long tenantId, PurchaseResourceEntity resource, PurchaseRelationEntity relation) {
        if (PurchaseResourceProcurementMode.NOT_REQUIRED.value().equals(resource.getProcurementMode())) {
            return BigDecimal.ZERO;
        }
        return referenceUnitPrice(tenantId, relation);
    }

    private List<ProductDesignerSupplierResponse> supplierResponses(Long tenantId, PurchaseResourceEntity resource) {
        return supplierResponsesByResource(tenantId, List.of(resource.getId()))
                .getOrDefault(resource.getId(), List.of());
    }

    private Map<Long, List<ProductDesignerSupplierResponse>> supplierResponsesByResource(
            Long tenantId,
            List<Long> resourceIds
    ) {
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        List<PurchaseRelationSupplierPriceRow> rows = relationMapper
                .selectActiveResourceSupplierPriceRows(tenantId, resourceIds);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PurchaseRelationSupplierPriceRow>> byResource = rows.stream()
                .collect(Collectors.groupingBy(
                        PurchaseRelationSupplierPriceRow::getResourceId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, List<ProductDesignerSupplierResponse>> result = new LinkedHashMap<>();
        byResource.forEach((resourceId, resourceRows) -> result.put(resourceId, supplierResponses(resourceRows)));
        return result;
    }

    private List<ProductDesignerSupplierResponse> supplierResponses(List<PurchaseRelationSupplierPriceRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(
                        PurchaseRelationSupplierPriceRow::getRelationId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(this::supplierResponse)
                .toList();
    }

    private ProductDesignerSupplierResponse supplierResponse(List<PurchaseRelationSupplierPriceRow> rows) {
        PurchaseRelationSupplierPriceRow relation = rows.getFirst();
        List<ProductDesignerSupplierPriceLineResponse> lines = rows.stream()
                .filter(item -> item.getResourceProjectId() != null)
                .map(item -> new ProductDesignerSupplierPriceLineResponse(
                        item.getResourceProjectId(), item.getProjectName(), money(item.getMarketPrice()),
                        money(item.getPeerPrice()), money(item.getTeamPrice())
                ))
                .toList();
        BigDecimal referencePrice = PRICE_MODE_UNIFIED.equals(relation.getPriceMode())
                ? money(relation.getUnifiedPrice())
                : rows.stream().map(this::bestLinePrice).filter(Objects::nonNull).findFirst().orElse(BigDecimal.ZERO);
        return new ProductDesignerSupplierResponse(
                relation.getRelationId(),
                relation.getSupplierId(),
                relation.getSupplierName(),
                Boolean.TRUE.equals(relation.getDefaultSupplier()),
                relation.getPriceMode(),
                money(relation.getUnifiedPrice()),
                referencePrice,
                lines
        );
    }

    private Map<Long, ProductDesignerSupplierResponse> defaultSupplierMap(Long tenantId, List<PurchaseResourceEntity> resources) {
        if (resources.isEmpty()) {
            return Map.of();
        }
        List<Long> resourceIds = resources.stream().map(PurchaseResourceEntity::getId).toList();
        Map<Long, List<ProductDesignerSupplierResponse>> responses = supplierResponsesByResource(tenantId, resourceIds);
        Map<Long, ProductDesignerSupplierResponse> result = new LinkedHashMap<>();
        responses.forEach((resourceId, suppliers) -> suppliers.stream().findFirst()
                .ifPresent(supplier -> result.put(resourceId, supplier)));
        return result;
    }

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

    private BigDecimal referenceUnitPrice(Long tenantId, PurchaseRelationEntity relation) {
        if (relation == null) {
            return BigDecimal.ZERO;
        }
        if (PRICE_MODE_UNIFIED.equals(relation.getPriceMode())) {
            return money(relation.getUnifiedPrice());
        }
        List<SupplierResourcePriceEntity> prices = priceMapper.selectList(new QueryWrapper<SupplierResourcePriceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .eq("relation_id", relation.getId())
                .orderByAsc("resource_project_id")
                .orderByAsc("id"));
        return prices.stream().map(this::bestLinePrice).filter(Objects::nonNull).findFirst().orElse(BigDecimal.ZERO);
    }

    private BigDecimal bestLinePrice(SupplierResourcePriceEntity line) {
        if (line.getTeamPrice() != null) {
            return line.getTeamPrice();
        }
        if (line.getPeerPrice() != null) {
            return line.getPeerPrice();
        }
        return line.getMarketPrice();
    }

    private BigDecimal bestLinePrice(PurchaseRelationSupplierPriceRow line) {
        if (line.getTeamPrice() != null) {
            return line.getTeamPrice();
        }
        if (line.getPeerPrice() != null) {
            return line.getPeerPrice();
        }
        return line.getMarketPrice();
    }

    private int nextSortOrder(Long tenantId, Long productId, Integer dayNo) {
        List<SalesProductDayResourceEntity> current = dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId)
                .eq("day_no", dayNo)
                .orderByDesc("sort_order")
                .last("limit 1"));
        if (current.isEmpty() || current.get(0).getSortOrder() == null) {
            return 1;
        }
        return current.get(0).getSortOrder() + 1;
    }

    private SalesProductAdultQuoteEntity currentAdultQuote(Long tenantId, Long productId) {
        return adultQuoteMapper.selectList(new QueryWrapper<SalesProductAdultQuoteEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .orderByDesc("id")
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private BigDecimal productCost(Long tenantId, Long productId) {
        return dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId))
                .stream()
                .map(SalesProductDayResourceEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
