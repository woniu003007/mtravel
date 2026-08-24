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
import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import com.mtravel.platform.purchase.relation.optional.mapper.PurchaseRelationOptionalItemMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceProcurementMode;
import com.mtravel.platform.purchase.resource.enums.ScenicLevel;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionImageEntity;
import com.mtravel.platform.purchase.resource.material.service.ResourceIntroductionExtensionBlockCodec;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.resource.optional.dto.PurchaseResourceOptionalItemResponse;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayPlanResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayDestinationResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayDestinationSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayEndImageSelectionRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayEndImageSelectionResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayItineraryResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayItinerarySaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceDeleteRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceReorderRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceSupplierSaveRequest;
import com.mtravel.platform.sales.product.designer.enums.ProductDesignerArrangementRole;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerBreakfastHotelResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerBreakfastPlanResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayItinerarySaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayItineraryResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayWordPlanMaterialRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayWordPlanMaterialResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayWordPlanResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayWordPlanResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayWordPlanSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDetailResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerIntroductionResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerIntroductionSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerIntroductionSnapshotResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerMapResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerOptionalItemsSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerResourceDetailResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerResourceImageResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedMaterialRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedMaterialResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSupplierPriceLineResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSupplierResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSupplierOptionalItemResponse;
import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceImageEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceIntroductionEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceIntroductionMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String ARRANGEMENT_ITINERARY = ProductDesignerArrangementRole.ITINERARY.value();
    private static final String ARRANGEMENT_ACCOMMODATION = ProductDesignerArrangementRole.ACCOMMODATION.value();
    private static final String ARRANGEMENT_BREAKFAST = ProductDesignerArrangementRole.BREAKFAST.value();
    private static final String ARRANGEMENT_LUNCH = ProductDesignerArrangementRole.LUNCH.value();
    private static final String ARRANGEMENT_DINNER = ProductDesignerArrangementRole.DINNER.value();
    private static final String ARRANGEMENT_GROUND_SERVICE = ProductDesignerArrangementRole.GROUND_SERVICE.value();
    private static final String ARRANGEMENT_UNASSIGNED = ProductDesignerArrangementRole.LEGACY_UNASSIGNED.value();
    private static final Set<String> MEAL_ARRANGEMENT_ROLES = Set.of(
            ARRANGEMENT_BREAKFAST, ARRANGEMENT_LUNCH, ARRANGEMENT_DINNER);

    private final SalesProductMapper productMapper;
    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseRelationMapper relationMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierResourcePriceMapper priceMapper;
    private final PurchaseResourceIntroductionMapper introductionMapper;
    private final PurchaseResourceImageMapper imageMapper;
    private final SalesProductDayResourceMapper dayResourceMapper;
    private final SalesProductDayResourceImageMapper dayResourceImageMapper;
    private final SalesProductDayResourceIntroductionMapper dayResourceIntroductionMapper;
    private final SalesProductAdultQuoteMapper adultQuoteMapper;
    private final SalesProductDocumentVersionMapper documentVersionMapper;
    @Autowired(required = false)
    private PurchaseRelationOptionalItemMapper relationOptionalItemMapper;
    /** 延迟字段注入避免改变已有构造契约；仅在请求明确带自费项目时写入快照。 */
    @Autowired(required = false)
    private SalesProductDesignerOptionalItemService optionalItemService;
    @Autowired(required = false)
    private com.mtravel.platform.purchase.resource.optional.service.PurchaseResourceOptionalItemService resourceOptionalItemService;
    /** 当天 Word 方案批量读取景区自费项目，避免逐个景区调用资源自费项目服务。 */
    @Autowired(required = false)
    private PurchaseResourceOptionalItemMapper resourceOptionalItemMapper;
    /** 介绍素材图片在产品编辑时一次性带出，避免按素材逐条查询。 */
    @Autowired(required = false)
    private PurchaseResourceIntroductionImageMapper introductionImageMapper;
    /** 扩展内容块用于产品预览与快照，保留字段注入以兼容既有构造测试。 */
    @Autowired(required = false)
    private ResourceIntroductionExtensionBlockCodec extensionBlockCodec;
    /** 图片展示方式属于每日行程，字段注入保持现有单元测试构造契约不变。 */
    @Autowired(required = false)
    private SalesProductItineraryDayMapper itineraryDayMapper;
    /** 新旧构造测试不变；运行时优先使用统一报价解析器冻结供应商关系和价格。 */
    @Autowired(required = false)
    private ProductDesignerSupplierQuoteService supplierQuoteService;
    /** 产品级全程用车只在设计草稿中汇总一次，不下沉到每日资源。 */
    @Autowired(required = false)
    private SalesProductDesignerVehicleArrangementService vehicleArrangementService;

    private static final Set<String> WORD_IMAGE_MODES = Set.of("follow_resource", "day_end", "hidden");
    private static final int WORD_IMAGE_MIN_COUNT = 2;
    private static final int WORD_IMAGE_MAX_COUNT = 3;

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
            SalesProductDayResourceIntroductionMapper dayResourceIntroductionMapper,
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
        this.dayResourceIntroductionMapper = dayResourceIntroductionMapper;
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
                .ne("arrangement_role", ARRANGEMENT_GROUND_SERVICE)
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
                .eq("product_id", draftId)
                .ne("arrangement_role", ARRANGEMENT_GROUND_SERVICE));
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
        if (vehicleArrangementService != null) {
            vehicleArrangementService.softDeleteForProduct(tenantId, draftId, operator);
        }

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
                .ne("arrangement_role", ARRANGEMENT_GROUND_SERVICE)
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
        Map<Long, List<SalesProductDayResourceIntroductionEntity>> selectedIntroductions =
                selectedIntroductions(tenantId, productId, resources);
        Map<Long, List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse>>
                selectedOptionalItems = optionalItemService == null
                ? Map.of()
                : optionalItemService.listByDayResourceIds(
                        tenantId,
                        productId,
                        resources.stream().map(SalesProductDayResourceEntity::getId).toList()
                );
        Map<Integer, SalesProductItineraryDayEntity> itineraryByDay = itineraryDayMapper == null
                ? Map.of()
                : itineraryDayMapper.selectList(new QueryWrapper<SalesProductItineraryDayEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .orderByAsc("day_no")
                        .orderByAsc("id")
                ).stream().collect(Collectors.toMap(
                        SalesProductItineraryDayEntity::getDayNo,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        int daysCount = product.getTravelDays() == null ? 1 : product.getTravelDays();
        List<ProductDesignerDayPlanResponse> days = java.util.stream.IntStream.rangeClosed(1, daysCount)
                .mapToObj(dayNo -> dayPlan(
                        dayNo,
                        byDay.getOrDefault(dayNo, List.of()),
                        byDay.getOrDefault(dayNo - 1, List.of()),
                        selectedImageIds,
                        selectedIntroductions,
                        selectedOptionalItems,
                        itineraryByDay.get(dayNo)
                ))
                .toList();
        BigDecimal dayResourceCost = resources.stream()
                .map(SalesProductDayResourceEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vehicleCost = vehicleArrangementService == null
                ? BigDecimal.ZERO
                : vehicleArrangementService.costAmount(tenantId, productId);
        BigDecimal totalCost = dayResourceCost.add(vehicleCost);
        return new ProductDesignerDetailResponse(
                product.getId(),
                product.getProductName(),
                product.getProvince(),
                product.getCity(),
                daysCount,
                product.getStatus(),
                money(totalCost),
                money(dayResourceCost),
                money(vehicleCost),
                vehicleArrangementService == null ? List.of() : vehicleArrangementService.list(tenantId, productId),
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
                .notIn("resource_type", List.of("ground_agent", "traffic", "vehicle"))
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
                && !LEVEL_RESOURCE_TYPES.contains(resourceType)) {
            throw new BizException("星级/接待标准筛选只能用于酒店或餐厅");
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
        Map<Long, List<Long>> introductionImageIds = introductionImageIdsByIntroduction(
                tenantId, introductions, images
        );
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
                introductions.stream()
                        .map(item -> ProductDesignerIntroductionResponse.fromEntity(
                                item, introductionImageIds.getOrDefault(item.getId(), List.of()),
                                extensionBlocks(item.getExtensionBlocks())
                        ))
                        .toList(),
                images.stream().map(ProductDesignerResourceImageResponse::fromEntity).toList(),
                suppliers,
                defaultSupplierId,
                "scenic".equals(resource.getResourceType()) && resourceOptionalItemService != null
                        ? resourceOptionalItemService.list(tenantId, resourceId) : List.of()
        );
    }

    /**
     * 批量组装当天方案所需的资源候选详情。
     *
     * <p>当天方案会同时展示多个景区的介绍、图片、供应商和自费项目。这里先按资源集合
     * 批量读取，再在内存中按资源归组，避免为每个景区重复执行同一组查询。</p>
     */
    private Map<Long, ProductDesignerResourceDetailResponse> resourceDetails(
            Long tenantId,
            List<Long> resourceIds
    ) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        List<PurchaseResourceEntity> resources = resourceMapper.selectList(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .in("id", resourceIds));
        if (resources.isEmpty()) {
            return Map.of();
        }
        Map<Long, PurchaseResourceEntity> resourceById = resources.stream()
                .collect(Collectors.toMap(PurchaseResourceEntity::getId, Function.identity()));
        List<PurchaseResourceIntroductionEntity> introductions = introductionMapper.selectList(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("status", INTRODUCTION_PUBLISHED)
                        .in("resource_id", resourceIds)
                        .orderByDesc("published_at")
                        .orderByDesc("id")
        );
        Map<Long, List<PurchaseResourceIntroductionEntity>> introductionsByResource = introductions.stream()
                .collect(Collectors.groupingBy(
                        PurchaseResourceIntroductionEntity::getResourceId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<PurchaseResourceImageEntity> images = imageMapper.selectList(new QueryWrapper<PurchaseResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .in("resource_id", resourceIds)
                .orderByDesc("is_cover")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
        Map<Long, List<PurchaseResourceImageEntity>> imagesByResource = images.stream()
                .collect(Collectors.groupingBy(
                        PurchaseResourceImageEntity::getResourceId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, List<Long>> introductionImageIds = introductionImageIdsByIntroduction(
                tenantId, introductions, imagesByResource
        );
        Map<Long, List<ProductDesignerSupplierResponse>> suppliersByResource =
                supplierResponsesByResource(tenantId, resourceIds);
        Map<Long, List<PurchaseResourceOptionalItemResponse>> optionalItemsByResource =
                optionalItemsByResource(tenantId, resourceIds);

        Map<Long, ProductDesignerResourceDetailResponse> result = new LinkedHashMap<>();
        for (Long resourceId : resourceIds) {
            PurchaseResourceEntity resource = resourceById.get(resourceId);
            if (resource == null) {
                continue;
            }
            List<PurchaseResourceIntroductionEntity> resourceIntroductions =
                    introductionsByResource.getOrDefault(resourceId, List.of());
            List<PurchaseResourceImageEntity> resourceImages = imagesByResource.getOrDefault(resourceId, List.of());
            List<ProductDesignerSupplierResponse> suppliers = suppliersByResource.getOrDefault(resourceId, List.of());
            Long defaultSupplierId = suppliers.stream()
                    .filter(item -> Boolean.TRUE.equals(item.isDefault()))
                    .map(ProductDesignerSupplierResponse::supplierId)
                    .findFirst()
                    .orElse(null);
            result.put(resourceId, new ProductDesignerResourceDetailResponse(
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
                    resourceIntroductions.stream()
                            .map(item -> ProductDesignerIntroductionResponse.fromEntity(
                                    item,
                                    introductionImageIds.getOrDefault(item.getId(), List.of()),
                                    extensionBlocks(item.getExtensionBlocks())
                            ))
                            .toList(),
                    resourceImages.stream().map(ProductDesignerResourceImageResponse::fromEntity).toList(),
                    suppliers,
                    defaultSupplierId,
                    "scenic".equals(resource.getResourceType())
                            ? optionalItemsByResource.getOrDefault(resourceId, List.of())
                            : List.of()
            ));
        }
        return result;
    }

    /**
     * 查询当天景区组合的 Word 方案。
     *
     * <p>素材以一个有序流返回，允许介绍素材和自费项目跨景区拖动；旧数据仍按景区顺序、景区内素材顺序展开。</p>
     */
    public ProductDesignerDayWordPlanResponse dayWordPlan(Long tenantId, Long productId, Integer dayNo) {
        SalesProductEntity product = loadProduct(tenantId, productId);
        validateDayNo(product, dayNo);
        List<SalesProductDayResourceEntity> dayResources = scenicDayResources(tenantId, productId, dayNo);
        Map<Long, List<Long>> selectedImageIds = selectedImageIds(tenantId, productId, dayResources);
        Map<Long, List<SalesProductDayResourceIntroductionEntity>> selectedIntroductions =
                selectedIntroductions(tenantId, productId, dayResources);
        Map<Long, List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse>>
                selectedOptionalItems = optionalItemService == null
                ? Map.of()
                : optionalItemService.listByDayResourceIds(
                        tenantId,
                        productId,
                        dayResources.stream().map(SalesProductDayResourceEntity::getId).toList()
                );
        Map<Long, ProductDesignerResourceDetailResponse> resourceDetails = resourceDetails(
                tenantId,
                dayResources.stream().map(SalesProductDayResourceEntity::getResourceId).distinct().toList()
        );
        List<ProductDesignerDayWordPlanResourceResponse> resources = dayResources.stream()
                .map(dayResource -> new ProductDesignerDayWordPlanResourceResponse(
                        dayResourceResponse(
                                dayResource,
                                selectedImageIds.getOrDefault(dayResource.getId(), List.of()),
                                selectedIntroductions.getOrDefault(dayResource.getId(), List.of()),
                                selectedOptionalItems.getOrDefault(dayResource.getId(), List.of())
                        ),
                        resourceDetails.get(dayResource.getResourceId())
                ))
                .toList();
        String imageMode = currentWordImageMode(tenantId, productId, dayNo);
        return new ProductDesignerDayWordPlanResponse(
                productId,
                dayNo,
                resources,
                wordPlanMaterials(resources),
                imageMode,
                "day_end".equals(imageMode)
                        ? dayEndImageSelections(tenantId, productId, dayResources)
                        : List.of()
        );
    }

    /**
     * 整组保存当天景区的 Word 素材方案。
     *
     * <p>供应商、成本和当天景区排序不在此处改动；任一素材、报价或归属校验失败时整个方案回滚。</p>
     */
    @Transactional
    public ProductDesignerDayWordPlanResponse saveDayWordPlan(
            Long tenantId,
            ProductDesignerDayWordPlanSaveRequest request,
            String operator
    ) {
        SalesProductEntity product = loadProduct(tenantId, request.productId());
        validateDayNo(product, request.dayNo());
        List<SalesProductDayResourceEntity> dayResources = scenicDayResources(tenantId, request.productId(), request.dayNo());
        Map<Long, SalesProductDayResourceEntity> dayResourceById = dayResources.stream()
                .collect(Collectors.toMap(SalesProductDayResourceEntity::getId, Function.identity()));
        Set<Long> requestedIds = new java.util.LinkedHashSet<>(request.dayResourceIds());
        if (requestedIds.size() != request.dayResourceIds().size() || !requestedIds.equals(dayResourceById.keySet())) {
            throw new BizException("景区组合已变更，请刷新后重新保存");
        }
        String imageMode = normalizeWordImageMode(request.imageMode());
        if ("day_end".equals(imageMode)) {
            saveDayEndImageSelectionsIfProvided(tenantId, request, dayResources, operator);
        } else if ("follow_resource".equals(imageMode)) {
            validateWordPlanImageSelections(request, dayResources);
        }
        saveWordImageMode(tenantId, request.productId(), request.dayNo(), imageMode);

        record OrderedMaterial(ProductDesignerDayWordPlanMaterialRequest value, int sortOrder) {}
        List<OrderedMaterial> orderedMaterials = java.util.stream.IntStream
                .range(0, request.selectedMaterials() == null ? 0 : request.selectedMaterials().size())
                .mapToObj(index -> new OrderedMaterial(request.selectedMaterials().get(index), index + 1))
                .toList();
        Map<Long, List<OrderedMaterial>> materialsByDayResource = orderedMaterials.stream()
                .peek(item -> {
                    if (!dayResourceById.containsKey(item.value().dayResourceId())) {
                        throw new BizException("素材不属于当前景区组合");
                    }
                })
                .collect(Collectors.groupingBy(item -> item.value().dayResourceId(), LinkedHashMap::new, Collectors.toList()));

        for (SalesProductDayResourceEntity dayResource : dayResources) {
            PurchaseResourceEntity resource = loadActiveResource(tenantId, dayResource.getResourceId());
            List<OrderedMaterial> selected = materialsByDayResource.getOrDefault(dayResource.getId(), List.of());
            List<ProductDesignerSelectedMaterialRequest> materials = selected.stream()
                    .map(item -> new ProductDesignerSelectedMaterialRequest(
                            item.value().materialType(),
                            item.value().introductionId(),
                            item.value().resourceOptionalItemId(),
                            item.value().supplierOptionalItemId(),
                            item.value().salePrice()
                    ))
                    .toList();
            List<PurchaseResourceIntroductionEntity> introductions =
                    standardIntroductionsForMaterials(tenantId, resource.getId(), materials);
            List<Integer> introductionSortOrders = selected.stream()
                    .filter(item -> "introduction".equals(item.value().materialType()))
                    .map(OrderedMaterial::sortOrder)
                    .toList();
            saveIntroductionSnapshots(tenantId, dayResource, introductions, introductionSortOrders, operator);
            if ("follow_resource".equals(imageMode)) {
                List<Long> explicitImageIds = request.selectedImageIdsByResource() == null
                        ? null
                        : request.selectedImageIdsByResource().get(dayResource.getId());
                if (explicitImageIds != null) {
                    saveSelectedImages(tenantId, dayResource, resource, explicitImageIds, operator);
                } else {
                    saveSelectedImagesForMaterials(tenantId, dayResource, resource, materials, operator);
                }
            }

            if (optionalItemService == null) {
                if (materials.stream().anyMatch(item -> "optional_item".equals(item.materialType()))) {
                    throw new BizException("自费项目服务未配置");
                }
                continue;
            }
            List<ProductDesignerSelectedOptionalItemRequest> optionalRows = optionalMaterialRows(materials);
            List<Integer> optionalSortOrders = selected.stream()
                    .filter(item -> "optional_item".equals(item.value().materialType()))
                    .map(OrderedMaterial::sortOrder)
                    .toList();
            optionalItemService.saveWithGlobalSortOrders(tenantId,
                    new ProductDesignerOptionalItemsSaveRequest(request.productId(), dayResource.getId(), optionalRows),
                    operator, optionalRows, optionalSortOrders);
        }
        return dayWordPlan(tenantId, request.productId(), request.dayNo());
    }

    private String currentWordImageMode(Long tenantId, Long productId, Integer dayNo) {
        if (itineraryDayMapper == null) return "follow_resource";
        SalesProductItineraryDayEntity day = itineraryDayMapper.selectOne(new QueryWrapper<SalesProductItineraryDayEntity>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .eq("day_no", dayNo)
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        return normalizeWordImageMode(day == null ? null : day.getWordImageMode());
    }

    private String normalizeWordImageMode(String value) {
        if (value == null || value.isBlank()) return "follow_resource";
        if (!WORD_IMAGE_MODES.contains(value)) {
            throw new BizException("图片展示方式不合法");
        }
        return value;
    }

    /**
     * 产品 Word 图片以景区为单位排版：允许不选，选择时必须为 2 或 3 张，避免单图或过多图片破坏版面。
     */
    private void validateWordPlanImageSelections(
            ProductDesignerDayWordPlanSaveRequest request,
            List<SalesProductDayResourceEntity> dayResources
    ) {
        if (request.selectedImageIdsByResource() == null) return;
        Set<Long> currentDayResourceIds = dayResources.stream()
                .map(SalesProductDayResourceEntity::getId)
                .collect(Collectors.toSet());
        if (request.selectedImageIdsByResource().keySet().stream().anyMatch(id -> id == null || !currentDayResourceIds.contains(id))) {
            throw new BizException("图片不属于当前景区组合");
        }
        for (SalesProductDayResourceEntity dayResource : dayResources) {
            List<Long> imageIds = request.selectedImageIdsByResource().get(dayResource.getId());
            if (imageIds == null) continue;
            int count = (int) imageIds.stream().filter(Objects::nonNull).distinct().count();
            if (count > 0 && count < WORD_IMAGE_MIN_COUNT) {
                throw new BizException(dayResource.getResourceNameSnapshot() + " 已选 1 张图片，请补充至 2 或 3 张，或清空图片后保存");
            }
            if (count > WORD_IMAGE_MAX_COUNT) {
                throw new BizException(dayResource.getResourceNameSnapshot() + " 最多只能选择 3 张图片");
            }
        }
    }

    /**
     * day_end 使用当天一组图片，排序可跨景区；未提交任一图片字段时保持历史快照不变。
     */
    private void saveDayEndImageSelectionsIfProvided(
            Long tenantId,
            ProductDesignerDayWordPlanSaveRequest request,
            List<SalesProductDayResourceEntity> dayResources,
            String operator
    ) {
        List<ProductDesignerDayEndImageSelectionRequest> selections = request.dayEndImageSelections();
        if (selections == null && request.selectedImageIdsByResource() == null) {
            return;
        }
        if (selections == null) {
            selections = legacyDayEndImageSelections(request.selectedImageIdsByResource(), dayResources);
        }
        Map<Long, SalesProductDayResourceEntity> resourceById = dayResources.stream()
                .collect(Collectors.toMap(SalesProductDayResourceEntity::getId, Function.identity()));
        Set<String> distinct = new java.util.HashSet<>();
        for (ProductDesignerDayEndImageSelectionRequest selection : selections) {
            if (selection == null || !resourceById.containsKey(selection.dayResourceId())
                    || selection.imageId() == null
                    || !distinct.add(selection.dayResourceId() + ":" + selection.imageId())) {
                throw new BizException("当天末尾图片不属于当前景区组合或重复选择");
            }
        }
        if (selections.size() == 1 || selections.size() > WORD_IMAGE_MAX_COUNT) {
            throw new BizException("当天末尾图片只能选择 0、2 或 3 张");
        }
        Map<Long, List<Integer>> sortOrdersByResource = new LinkedHashMap<>();
        Map<Long, List<Long>> imageIdsByResource = new LinkedHashMap<>();
        for (int index = 0; index < selections.size(); index += 1) {
            ProductDesignerDayEndImageSelectionRequest selection = selections.get(index);
            imageIdsByResource.computeIfAbsent(selection.dayResourceId(), ignored -> new java.util.ArrayList<>())
                    .add(selection.imageId());
            sortOrdersByResource.computeIfAbsent(selection.dayResourceId(), ignored -> new java.util.ArrayList<>())
                    .add(index + 1);
        }
        for (SalesProductDayResourceEntity dayResource : dayResources) {
            PurchaseResourceEntity resource = loadActiveResource(tenantId, dayResource.getResourceId());
            saveSelectedImages(tenantId, dayResource, resource,
                    imageIdsByResource.getOrDefault(dayResource.getId(), List.of()),
                    sortOrdersByResource.getOrDefault(dayResource.getId(), List.of()), operator);
        }
    }

    private List<ProductDesignerDayEndImageSelectionRequest> legacyDayEndImageSelections(
            Map<Long, List<Long>> imageIdsByResource,
            List<SalesProductDayResourceEntity> dayResources
    ) {
        List<ProductDesignerDayEndImageSelectionRequest> selections = new java.util.ArrayList<>();
        for (SalesProductDayResourceEntity resource : dayResources) {
            for (Long imageId : imageIdsByResource.getOrDefault(resource.getId(), List.of())) {
                if (imageId != null) {
                    selections.add(new ProductDesignerDayEndImageSelectionRequest(resource.getId(), imageId));
                }
            }
        }
        return selections;
    }

    /** 读取 day_end 的资源-图片对应关系，按跨资源全局排序回显。 */
    private List<ProductDesignerDayEndImageSelectionResponse> dayEndImageSelections(
            Long tenantId,
            Long productId,
            List<SalesProductDayResourceEntity> dayResources
    ) {
        List<Long> dayResourceIds = dayResources.stream().map(SalesProductDayResourceEntity::getId).toList();
        if (dayResourceIds.isEmpty()) {
            return List.of();
        }
        return dayResourceImageMapper.selectList(new QueryWrapper<SalesProductDayResourceImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .in("day_resource_id", dayResourceIds)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(item -> new ProductDesignerDayEndImageSelectionResponse(
                        item.getDayResourceId(), item.getResourceImageId(), item.getSortOrder()))
                .toList();
    }

    private void saveWordImageMode(Long tenantId, Long productId, Integer dayNo, String imageMode) {
        if (itineraryDayMapper == null) return;
        SalesProductItineraryDayEntity update = new SalesProductItineraryDayEntity();
        update.setWordImageMode(imageMode);
        itineraryDayMapper.update(update, new UpdateWrapper<SalesProductItineraryDayEntity>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .eq("day_no", dayNo)
                .eq("is_deleted", false));
    }

    /** 保存产品设计工作台当天住宿城市和三餐，复用每日行程表供产品 Word 输出。 */
    @Transactional
    public ProductDesignerDayItineraryResponse saveDayItinerary(
            Long tenantId,
            ProductDesignerDayItinerarySaveRequest request,
            String operator
    ) {
        SalesProductEntity product = loadProduct(tenantId, request.productId());
        validateDayNo(product, request.dayNo());
        if (itineraryDayMapper == null) {
            throw new BizException("每日行程服务未配置");
        }
        SalesProductItineraryDayEntity entity = itineraryDayMapper.selectOne(new QueryWrapper<SalesProductItineraryDayEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", request.productId())
                .eq("day_no", request.dayNo())
                .last("LIMIT 1"));
        boolean created = entity == null;
        if (created) {
            entity = new SalesProductItineraryDayEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(request.productId());
            entity.setDayNo(request.dayNo());
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
        }
        entity.setRelatedHotel(clean(request.accommodationCity()));
        entity.setBreakfastIncluded(Boolean.TRUE.equals(request.breakfastIncluded()));
        entity.setLunchIncluded(Boolean.TRUE.equals(request.lunchIncluded()));
        entity.setDinnerIncluded(Boolean.TRUE.equals(request.dinnerIncluded()));
        if (created) {
            itineraryDayMapper.insert(entity);
        } else {
            itineraryDayMapper.update(entity, new UpdateWrapper<SalesProductItineraryDayEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("is_deleted", false)
                    .eq("product_id", request.productId())
                    .eq("day_no", request.dayNo()));
        }
        return ProductDesignerDayItineraryResponse.fromEntity(entity, request.dayNo());
    }

    /**
     * 只保存当天主行程城市，不触碰住宿、用餐或已编排资源。
     * 地图筛选范围可以随城市切换，但异地资源快照仍完整保留。
     */
    @Transactional
    public ProductDesignerDayDestinationResponse saveDayDestination(
            Long tenantId,
            ProductDesignerDayDestinationSaveRequest request,
            String operator
    ) {
        SalesProductEntity product = loadProduct(tenantId, request.productId());
        validateDayNo(product, request.dayNo());
        if (itineraryDayMapper == null) {
            throw new BizException("每日行程服务未配置");
        }
        SalesProductItineraryDayEntity entity = itineraryDayMapper.selectOne(new QueryWrapper<SalesProductItineraryDayEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", request.productId())
                .eq("day_no", request.dayNo())
                .last("LIMIT 1"));
        boolean created = entity == null;
        if (created) {
            entity = new SalesProductItineraryDayEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(request.productId());
            entity.setDayNo(request.dayNo());
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            entity.setBreakfastIncluded(false);
            entity.setLunchIncluded(false);
            entity.setDinnerIncluded(false);
        }
        entity.setDestinationProvince(clean(request.destinationProvince()));
        entity.setDestinationCity(clean(request.destinationCity()));
        entity.setDestinationDistrict(clean(request.destinationDistrict()));
        if (created) {
            itineraryDayMapper.insert(entity);
        } else {
            itineraryDayMapper.update(entity, new UpdateWrapper<SalesProductItineraryDayEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("is_deleted", false)
                    .eq("product_id", request.productId())
                    .eq("day_no", request.dayNo()));
        }
        return ProductDesignerDayDestinationResponse.fromEntity(entity, request.dayNo());
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
        SalesProductDayResourceEntity entity = request.id() == null
                ? new SalesProductDayResourceEntity()
                : loadDayResource(tenantId, request.productId(), request.id());
        String arrangementRole = normalizeArrangementRole(
                resource, request.arrangementRole(), entity.getArrangementRole());
        assertNotDuplicateDayResource(tenantId, request, arrangementRole);
        boolean hotelBreakfastIncluded = ARRANGEMENT_ACCOMMODATION.equals(arrangementRole)
                && Boolean.TRUE.equals(request.hotelBreakfastIncluded() == null
                ? entity.getHotelBreakfastIncluded()
                : request.hotelBreakfastIncluded());
        if (ARRANGEMENT_BREAKFAST.equals(arrangementRole)) {
            assertOrClearPreviousNightBreakfastHotels(
                    tenantId, request.productId(), request.dayNo(), request.replaceBreakfastSource(), operator);
        }
        if (hotelBreakfastIncluded) {
            assertOrClearNextDayBreakfastRestaurant(
                    tenantId, request.productId(), request.dayNo() + 1, request.replaceBreakfastSource(), operator);
        }
        ProductDesignerSupplierQuote quote = resolveSupplierQuote(
                tenantId, resource, request.supplierRelationId(), request.supplierId());
        List<Long> introductionIds = normalizedIntroductionIds(request);
        boolean selectedMaterialsProvided = request.selectedMaterials() != null;
        boolean legacyIntroductionUnchanged = request.introductionIds() == null
                && request.selectedIntroductionId() != null
                && Objects.equals(resource.getId(), entity.getResourceId())
                && Objects.equals(request.selectedIntroductionId(), entity.getSelectedIntroductionId());
        boolean introductionSelectionProvided = selectedMaterialsProvided || request.introductionIds() != null
                || (request.selectedIntroductionId() != null && !legacyIntroductionUnchanged)
                || request.id() == null;
        List<PurchaseResourceIntroductionEntity> introductions = selectedMaterialsProvided
                ? standardIntroductionsForMaterials(tenantId, resource.getId(), request.selectedMaterials())
                : introductionSelectionProvided
                        ? introductionsForSave(tenantId, resource.getId(), request, introductionIds)
                        : List.of();
        BigDecimal quantity = request.quantity() == null
                ? Objects.requireNonNullElse(entity.getQuantitySnapshot(), BigDecimal.ONE)
                : request.quantity();
        BigDecimal unitPrice = quote.unitPrice();
        BigDecimal costAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        entity.setTenantId(tenantId);
        entity.setProductId(request.productId());
        entity.setDayNo(request.dayNo());
        entity.setResourceId(resource.getId());
        entity.setResourceNameSnapshot(resource.getResourceName());
        entity.setResourceTypeSnapshot(resource.getResourceType());
        entity.setArrangementRole(arrangementRole);
        entity.setHotelBreakfastIncluded(hotelBreakfastIncluded);
        entity.setProvinceSnapshot(resource.getProvince());
        entity.setCitySnapshot(resource.getCity());
        entity.setDistrictSnapshot(resource.getDistrict());
        entity.setAddressSnapshot(resource.getAddress());
        entity.setLongitudeSnapshot(resource.getLongitude());
        entity.setLatitudeSnapshot(resource.getLatitude());
        entity.setProcurementModeSnapshot(resource.getProcurementMode());
        int fallbackSortOrder = entity.getSortOrder() == null
                ? nextSortOrder(tenantId, request.productId(), request.dayNo(), arrangementRole)
                : entity.getSortOrder();
        entity.setSortOrder(request.sortOrder() == null ? fallbackSortOrder : request.sortOrder());
        entity.setStayMinutes(request.stayMinutes() == null ? 0 : request.stayMinutes());
        entity.setIncludeInWord(request.includeInWord() == null || request.includeInWord());
        entity.setSupplierRelationIdSnapshot(quote.supplierRelationId());
        entity.setSupplierId(quote.supplierId());
        entity.setSupplierNameSnapshot(quote.supplierName());
        entity.setPriceModeSnapshot(quote.priceMode());
        entity.setUnitPriceSnapshot(unitPrice);
        entity.setQuantitySnapshot(quantity);
        entity.setCostAmountSnapshot(costAmount);
        if (introductionSelectionProvided) {
            applyIntroductionSnapshotForSave(entity, resource.getId(), introductions);
        }
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
        if (introductionSelectionProvided) {
            if (selectedMaterialsProvided) {
                saveIntroductionSnapshots(tenantId, entity, introductions,
                        standardMaterialSortOrders(request.selectedMaterials()), operator);
            } else {
                saveIntroductionSnapshots(tenantId, entity, introductions, operator);
            }
        }
        if (selectedMaterialsProvided) {
            saveSelectedImagesForMaterials(tenantId, entity, resource, request.selectedMaterials(), operator);
        } else {
            saveSelectedImages(tenantId, entity, resource, request.selectedImageIds(), operator);
        }
        // null 代表旧客户端未编辑自费项目，空列表代表用户明确清空。
        if (selectedMaterialsProvided) {
            if (optionalItemService == null) throw new BizException("自费项目服务未配置");
            List<ProductDesignerSelectedOptionalItemRequest> optionalRows = optionalMaterialRows(request.selectedMaterials());
            optionalItemService.saveWithGlobalSortOrders(tenantId, new ProductDesignerOptionalItemsSaveRequest(
                    request.productId(), entity.getId(), optionalRows), operator, optionalRows,
                    optionalMaterialSortOrders(request.selectedMaterials()));
        } else if (request.selectedOptionalItems() != null) {
            if (optionalItemService == null) throw new BizException("自费项目服务未配置");
            optionalItemService.save(tenantId, new ProductDesignerOptionalItemsSaveRequest(
                    request.productId(), entity.getId(), request.selectedOptionalItems()), operator);
        }
        return dayResourceResponse(tenantId, entity);
    }

    /** 仅更换一条已编排行资源的供应商关系，并刷新其成本快照。 */
    @Transactional
    public ProductDesignerDayResourceResponse changeDayResourceSupplier(
            Long tenantId, ProductDesignerDayResourceSupplierSaveRequest request, String operator
    ) {
        SalesProductDayResourceEntity entity = loadDayResource(tenantId, request.productId(), request.dayResourceId());
        PurchaseResourceEntity resource = loadActiveResource(tenantId, entity.getResourceId());
        ProductDesignerSupplierQuote quote = resolveSupplierQuote(
                tenantId, resource, request.supplierRelationId(), null);
        BigDecimal quantity = Objects.requireNonNullElse(entity.getQuantitySnapshot(), BigDecimal.ONE);
        entity.setSupplierRelationIdSnapshot(quote.supplierRelationId());
        entity.setSupplierId(quote.supplierId());
        entity.setSupplierNameSnapshot(quote.supplierName());
        entity.setPriceModeSnapshot(quote.priceMode());
        entity.setUnitPriceSnapshot(quote.unitPrice());
        entity.setCostAmountSnapshot(quote.unitPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP));
        int updated = dayResourceMapper.update(entity, baseDayResourceUpdate(tenantId, request.productId())
                .eq("id", request.dayResourceId()));
        if (updated == 0) {
            throw new BizException("产品每日资源不存在或已删除");
        }
        return dayResourceResponse(tenantId, entity);
    }

    /** 软删除产品某天的一条资源。 */
    @Transactional
    public void deleteDayResource(Long tenantId, ProductDesignerDayResourceDeleteRequest request, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        UpdateWrapper<SalesProductDayResourceEntity> wrapper = baseDayResourceUpdate(tenantId, request.productId())
                .eq("id", request.id())
                .set("is_deleted", true)
                .set("deleted_at", now)
                .set("deleted_by", operator);
        int updated = dayResourceMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BizException("产品每日资源不存在或已删除");
        }
        SalesProductDayResourceIntroductionEntity deletedIntroduction =
                new SalesProductDayResourceIntroductionEntity();
        markDeleted(deletedIntroduction, operator, OffsetDateTime.now());
        dayResourceIntroductionMapper.update(deletedIntroduction,
                new UpdateWrapper<SalesProductDayResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", request.productId())
                        .eq("day_resource_id", request.id()));
        if (optionalItemService != null) {
            optionalItemService.softDeleteForDayResource(
                    tenantId, request.productId(), request.id(), operator, now
            );
        }
    }

    /** 保存当前天资源顺序。 */
    @Transactional
    public void reorderDayResources(Long tenantId, ProductDesignerDayResourceReorderRequest request) {
        loadProduct(tenantId, request.productId());
        ProductDesignerArrangementRole arrangementRole =
                ProductDesignerArrangementRole.fromValue(request.arrangementRole());
        if (!arrangementRole.isIndependentlySortable()) {
            throw new BizException("当前资源区块不支持排序");
        }
        List<SalesProductDayResourceEntity> current = dayResourceMapper.selectList(new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", request.productId())
                .eq("day_no", request.dayNo())
                .eq("arrangement_role", arrangementRole.value()));
        Map<Long, SalesProductDayResourceEntity> currentMap = current.stream()
                .collect(Collectors.toMap(SalesProductDayResourceEntity::getId, Function.identity()));
        if (!currentMap.keySet().equals(new java.util.HashSet<>(request.dayResourceIds()))
                || request.dayResourceIds().size() != currentMap.size()) {
            throw new BizException("排序资源不属于当前产品或已删除");
        }
        for (int index = 0; index < request.dayResourceIds().size(); index += 1) {
            SalesProductDayResourceEntity entity = new SalesProductDayResourceEntity();
            entity.setSortOrder(index + 1);
            dayResourceMapper.update(entity, baseDayResourceUpdate(tenantId, request.productId())
                    .eq("day_no", request.dayNo())
                    .eq("arrangement_role", arrangementRole.value())
                    .eq("id", request.dayResourceIds().get(index)));
        }
    }

    /** 单独替换某条资源的介绍版本并更新正文快照。 */
    @Transactional
    public ProductDesignerDayResourceResponse saveIntroduction(Long tenantId, ProductDesignerIntroductionSaveRequest request) {
        SalesProductDayResourceEntity entity = loadDayResource(tenantId, request.productId(), request.dayResourceId());
        List<Long> introductionIds = normalizedIntroductionIds(request);
        List<PurchaseResourceIntroductionEntity> introductions =
                introductionsForSave(tenantId, entity.getResourceId(), request, introductionIds);
        applyIntroductionSnapshot(entity, introductions);
        dayResourceMapper.update(entity, baseDayResourceUpdate(tenantId, request.productId()).eq("id", request.dayResourceId()));
        saveIntroductionSnapshots(tenantId, entity, introductions, null);
        return dayResourceResponse(tenantId, entity);
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
            List<SalesProductDayResourceEntity> previousDayResources,
            Map<Long, List<Long>> selectedImageIds,
            Map<Long, List<SalesProductDayResourceIntroductionEntity>> selectedIntroductions,
            Map<Long, List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse>> selectedOptionalItems,
            SalesProductItineraryDayEntity itinerary
    ) {
        BigDecimal dayCost = resources.stream()
                .map(SalesProductDayResourceEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ProductDesignerDayResourceResponse> dayResources = resources.stream()
                .map(item -> {
                    List<SalesProductDayResourceIntroductionEntity> introductions =
                            effectiveIntroductionSnapshots(
                                    item,
                                    selectedIntroductions.getOrDefault(item.getId(), List.of())
                            );
                    return ProductDesignerDayResourceResponse.fromEntity(
                            item,
                            selectedImageIds.getOrDefault(item.getId(), List.of()),
                            introductions.stream()
                                    .map(SalesProductDayResourceIntroductionEntity::getResourceIntroductionId)
                                    .toList(),
                            introductions.stream()
                                    .map(snapshot -> ProductDesignerIntroductionSnapshotResponse.fromEntity(
                                            snapshot, extensionBlocks(snapshot.getExtensionBlocksSnapshot())
                                    ))
                                    .toList(),
                            selectedOptionalItems.getOrDefault(item.getId(), List.of()),
                            selectedMaterials(introductions,
                                    selectedOptionalItems.getOrDefault(item.getId(), List.of()))
                    );
                })
                .toList();
        ProductDesignerDayResourceResponse breakfastRestaurant = dayResources.stream()
                .filter(item -> ARRANGEMENT_BREAKFAST.equals(item.arrangementRole()))
                .findFirst()
                .orElse(null);
        List<ProductDesignerBreakfastHotelResponse> breakfastHotels = previousDayResources.stream()
                .filter(item -> ARRANGEMENT_ACCOMMODATION.equals(item.getArrangementRole()))
                .filter(item -> Boolean.TRUE.equals(item.getHotelBreakfastIncluded()))
                .map(item -> new ProductDesignerBreakfastHotelResponse(
                        item.getId(), item.getResourceId(), item.getResourceNameSnapshot()))
                .toList();
        ProductDesignerBreakfastPlanResponse mealPlan = new ProductDesignerBreakfastPlanResponse(
                breakfastRestaurant != null ? "restaurant" : breakfastHotels.isEmpty() ? "none" : "hotel",
                breakfastHotels,
                breakfastRestaurant
        );
        return new ProductDesignerDayPlanResponse(
                dayNo,
                itinerary == null ? null : itinerary.getDestinationProvince(),
                itinerary == null ? null : itinerary.getDestinationCity(),
                itinerary == null ? null : itinerary.getDestinationDistrict(),
                money(dayCost),
                dayResources,
                resources.stream()
                        .filter(item -> ARRANGEMENT_ACCOMMODATION.equals(item.getArrangementRole()))
                        .map(SalesProductDayResourceEntity::getCitySnapshot)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(itinerary == null ? null : itinerary.getRelatedHotel()),
                itinerary != null && Boolean.TRUE.equals(itinerary.getBreakfastIncluded()),
                itinerary != null && Boolean.TRUE.equals(itinerary.getLunchIncluded()),
                itinerary != null && Boolean.TRUE.equals(itinerary.getDinnerIncluded()),
                mealPlan
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

    private ProductDesignerDayResourceResponse dayResourceResponse(
            Long tenantId,
            SalesProductDayResourceEntity entity
    ) {
        List<SalesProductDayResourceIntroductionEntity> introductions =
                selectedIntroductionsFor(tenantId, entity.getProductId(), entity.getId());
        List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse> optionalItems =
                optionalItemService == null ? List.of() : optionalItemService.list(tenantId, entity.getProductId(), entity.getId());
        return dayResourceResponse(
                entity,
                selectedImageIdsFor(tenantId, entity.getId()),
                introductions,
                optionalItems
        );
    }

    /** 使用已批量加载的子数据组装当天资源响应。 */
    private ProductDesignerDayResourceResponse dayResourceResponse(
            SalesProductDayResourceEntity entity,
            List<Long> selectedImageIds,
            List<SalesProductDayResourceIntroductionEntity> selectedIntroductions,
            List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse> selectedOptionalItems
    ) {
        List<SalesProductDayResourceIntroductionEntity> introductions =
                effectiveIntroductionSnapshots(entity, selectedIntroductions);
        List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse> optionalItems =
                selectedOptionalItems == null ? List.of() : selectedOptionalItems;
        return ProductDesignerDayResourceResponse.fromEntity(
                entity,
                selectedImageIds == null ? List.of() : selectedImageIds,
                introductions.stream().map(SalesProductDayResourceIntroductionEntity::getResourceIntroductionId).toList(),
                introductions.stream().map(item -> ProductDesignerIntroductionSnapshotResponse.fromEntity(
                        item, extensionBlocks(item.getExtensionBlocksSnapshot())
                )).toList(),
                optionalItems,
                selectedMaterials(introductions, optionalItems)
        );
    }

    /** 将两个历史快照表合并为前端拖拽使用的一条有序素材流。 */
    private List<ProductDesignerSelectedMaterialResponse> selectedMaterials(
            List<SalesProductDayResourceIntroductionEntity> introductions,
            List<com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse> optionalItems
    ) {
        record OrderedMaterial(ProductDesignerSelectedMaterialResponse value, int typeRank) {}
        List<OrderedMaterial> materials = new java.util.ArrayList<>();
        for (SalesProductDayResourceIntroductionEntity introduction : introductions) {
            materials.add(new OrderedMaterial(new ProductDesignerSelectedMaterialResponse(
                    "introduction", introduction.getResourceIntroductionId(), null, null, null,
                    introduction.getSortOrder(), introduction.getTitleSnapshot(), introduction.getContentSnapshot(), null), 0));
        }
        for (com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse optional : optionalItems) {
            materials.add(new OrderedMaterial(new ProductDesignerSelectedMaterialResponse(
                    "optional_item", optional.introductionId(), optional.resourceOptionalItemId(),
                    optional.supplierOptionalItemId(), optional.salePrice(), optional.sortOrder(),
                    optional.projectName(), optional.introductionContent(), optional.projectName()), 1));
        }
        return materials.stream()
                .sorted(Comparator.comparing((OrderedMaterial item) ->
                                item.value().sortOrder() == null ? Integer.MAX_VALUE : item.value().sortOrder())
                        .thenComparing(OrderedMaterial::typeRank))
                .map(OrderedMaterial::value)
                .toList();
    }

    private List<SalesProductDayResourceIntroductionEntity> effectiveIntroductionSnapshots(
            SalesProductDayResourceEntity resource,
            List<SalesProductDayResourceIntroductionEntity> snapshots
    ) {
        if (snapshots != null && !snapshots.isEmpty()) {
            return snapshots;
        }
        if (resource.getSelectedIntroductionId() == null) {
            return List.of();
        }
        SalesProductDayResourceIntroductionEntity legacy = new SalesProductDayResourceIntroductionEntity();
        legacy.setResourceIntroductionId(resource.getSelectedIntroductionId());
        legacy.setIntroductionIndexVersion(resource.getIntroductionIndexVersion());
        legacy.setTitleSnapshot(resource.getIntroductionTitleSnapshot());
        legacy.setContentSnapshot(resource.getIntroductionContentSnapshot());
        legacy.setNoticeSnapshot(resource.getIntroductionNoticeSnapshot());
        legacy.setWarmTipSnapshot(resource.getIntroductionWarmTipSnapshot());
        legacy.setVisitDurationSnapshot(resource.getIntroductionVisitDurationSnapshot());
        legacy.setSortOrder(1);
        return List.of(legacy);
    }

    private Map<Long, List<SalesProductDayResourceIntroductionEntity>> selectedIntroductions(
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
        return dayResourceIntroductionMapper.selectList(new QueryWrapper<SalesProductDayResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .in("day_resource_id", dayResourceIds)
                        .orderByAsc("day_resource_id")
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        SalesProductDayResourceIntroductionEntity::getDayResourceId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<SalesProductDayResourceIntroductionEntity> selectedIntroductionsFor(
            Long tenantId,
            Long productId,
            Long dayResourceId
    ) {
        if (dayResourceId == null) {
            return List.of();
        }
        return dayResourceIntroductionMapper.selectList(new QueryWrapper<SalesProductDayResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .eq("day_resource_id", dayResourceId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .toList();
    }

    private void saveSelectedImages(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceEntity resource,
            List<Long> selectedImageIds,
            String operator
    ) {
        List<Long> normalizedImageIds = selectedImageIds == null ? null
                : selectedImageIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Integer> localSortOrders = normalizedImageIds == null ? List.of()
                : java.util.stream.IntStream.rangeClosed(1, normalizedImageIds.size()).boxed().toList();
        saveSelectedImages(tenantId, dayResource, resource, normalizedImageIds, localSortOrders, operator);
    }

    /** 单条资源图片保存；day_end 可传入跨资源的绝对排序，其他场景使用资源内排序。 */
    private void saveSelectedImages(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceEntity resource,
            List<Long> selectedImageIds,
            List<Integer> sortOrders,
            String operator
    ) {
        if (selectedImageIds == null || dayResource.getId() == null) {
            return;
        }
        List<Long> imageIds = selectedImageIds.stream().filter(Objects::nonNull).distinct().toList();
        if (imageIds.size() != selectedImageIds.size() || sortOrders == null || sortOrders.size() != imageIds.size()) {
            throw new BizException("图片选择或排序数据不完整");
        }
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
            selected.setSortOrder(sortOrders.get(index));
            selected.setCreatedBy(operator);
            selected.setIsDeleted(false);
            dayResourceImageMapper.insert(selected);
        }
    }

    /**
     * 新统一素材契约不接受前端手选图片：按素材顺序展开介绍关联图片并写入资源级图片快照。
     */
    private void saveSelectedImagesForMaterials(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            PurchaseResourceEntity resource,
            List<ProductDesignerSelectedMaterialRequest> materials,
            String operator
    ) {
        List<Long> introductionIds = materials.stream()
                .map(ProductDesignerSelectedMaterialRequest::introductionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (introductionIds.isEmpty()) {
            saveSelectedImages(tenantId, dayResource, resource, List.of(), operator);
            return;
        }
        if (introductionImageMapper == null) {
            throw new BizException("介绍素材图片服务未配置");
        }
        Map<Long, List<PurchaseResourceIntroductionImageEntity>> byIntroduction = introductionImageMapper.selectList(
                        new QueryWrapper<PurchaseResourceIntroductionImageEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("is_deleted", false)
                                .in("introduction_id", introductionIds)
                                .orderByAsc("introduction_id")
                                .orderByAsc("sort_order")
                                .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(PurchaseResourceIntroductionImageEntity::getIntroductionId,
                        LinkedHashMap::new, Collectors.toList()));
        java.util.LinkedHashSet<Long> imageIds = new java.util.LinkedHashSet<>();
        for (ProductDesignerSelectedMaterialRequest material : materials) {
            if (material.introductionId() == null) {
                continue;
            }
            byIntroduction.getOrDefault(material.introductionId(), List.of()).stream()
                    .map(PurchaseResourceIntroductionImageEntity::getResourceImageId)
                    .filter(Objects::nonNull)
                    .forEach(imageIds::add);
        }
        // 即使展开后没有图片，也明确清空历史手选图片。
        saveSelectedImages(tenantId, dayResource, resource, List.copyOf(imageIds), operator);
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

        SalesProductDayResourceIntroductionEntity deletedIntroduction =
                new SalesProductDayResourceIntroductionEntity();
        markDeleted(deletedIntroduction, operator, now);
        dayResourceIntroductionMapper.update(deletedIntroduction,
                new UpdateWrapper<SalesProductDayResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", draftId));

        if (optionalItemService != null) {
            optionalItemService.softDeleteForProduct(tenantId, draftId, operator, now);
        }

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

    /**
     * 查询某天已加入行程的景区，顺序与右侧行程保持一致。
     *
     * <p>Word 方案只处理景区，酒店、餐饮等资源仍使用各自的编辑入口。</p>
     */
    private List<SalesProductDayResourceEntity> scenicDayResources(
            Long tenantId,
            Long productId,
            Integer dayNo
    ) {
        return dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId)
                .eq("day_no", dayNo)
                .eq("resource_type_snapshot", "scenic")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
    }

    /**
     * 将各景区旧快照展开为一个可跨景区拖动的素材流。
     *
     * <p>新数据的 sort_order 是当天景区组内的绝对顺序；历史数据每个景区都会从 1
     * 开始，因此检测到重复或缺失排序后按“行程顺序 + 景区内部顺序”兼容展开。</p>
     */
    private List<ProductDesignerDayWordPlanMaterialResponse> wordPlanMaterials(
            List<ProductDesignerDayWordPlanResourceResponse> resources
    ) {
        record IndexedMaterial(ProductDesignerDayWordPlanMaterialResponse value, int resourceIndex,
                               int materialIndex, Integer sortOrder) {}
        List<IndexedMaterial> values = new java.util.ArrayList<>();
        for (int resourceIndex = 0; resourceIndex < resources.size(); resourceIndex += 1) {
            ProductDesignerDayWordPlanResourceResponse resource = resources.get(resourceIndex);
            List<ProductDesignerSelectedMaterialResponse> materials =
                    Objects.requireNonNullElse(resource.dayResource().selectedMaterials(), List.of());
            for (int materialIndex = 0; materialIndex < materials.size(); materialIndex += 1) {
                ProductDesignerSelectedMaterialResponse material = materials.get(materialIndex);
                values.add(new IndexedMaterial(
                        new ProductDesignerDayWordPlanMaterialResponse(
                                resource.dayResource().id(), resource.dayResource().resourceId(),
                                resource.dayResource().resourceName(), material
                        ), resourceIndex, materialIndex, material.sortOrder()));
            }
        }
        Set<Integer> sortOrders = values.stream()
                .map(IndexedMaterial::sortOrder)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasGlobalOrder = sortOrders.size() == values.size()
                && values.stream().allMatch(item -> item.sortOrder() != null);
        Comparator<IndexedMaterial> comparator = hasGlobalOrder
                ? Comparator.comparing(IndexedMaterial::sortOrder)
                : Comparator.comparing(IndexedMaterial::resourceIndex)
                        .thenComparing(IndexedMaterial::materialIndex);
        return values.stream().sorted(comparator).map(IndexedMaterial::value).toList();
    }

    private void validateDayNo(SalesProductEntity product, Integer dayNo) {
        int travelDays = product.getTravelDays() == null ? 1 : product.getTravelDays();
        if (dayNo == null || dayNo < 1 || dayNo > travelDays) {
            throw new BizException("行程天数超出产品天数范围");
        }
    }

    private void assertNotDuplicateDayResource(
            Long tenantId,
            ProductDesignerDayResourceSaveRequest request,
            String arrangementRole
    ) {
        // 地接服务是当天独立执行序列，同一资源允许按不同批次/时段重复安排。
        if (ARRANGEMENT_GROUND_SERVICE.equals(arrangementRole)) {
            return;
        }
        Long count = dayResourceMapper.selectCount(baseDayResourceQuery(tenantId, request.productId())
                .eq("day_no", request.dayNo())
                .eq("resource_id", request.resourceId())
                .eq("arrangement_role", arrangementRole)
                .ne(request.id() != null, "id", request.id()));
        if (count != null && count > 0) {
            throw new BizException("同一天不能重复加入同一个资源");
        }
        if (!MEAL_ARRANGEMENT_ROLES.contains(arrangementRole)
                && !ARRANGEMENT_ACCOMMODATION.equals(arrangementRole)) {
            return;
        }
        Long occupied = dayResourceMapper.selectCount(baseDayResourceQuery(tenantId, request.productId())
                .eq("day_no", request.dayNo())
                .eq("arrangement_role", arrangementRole)
                .ne(request.id() != null, "id", request.id()));
        if (occupied != null && occupied > 0) {
            if (ARRANGEMENT_ACCOMMODATION.equals(arrangementRole)) {
                throw new BizException("当天已安排酒店，请更换或删除原酒店");
            }
            throw new BizException("当天该餐次已安排餐厅，请先更换或删除原餐厅");
        }
    }

    /** 将资源类型约束为明确的住宿或餐次位置，避免餐厅仅以普通资源混入行程。 */
    private String normalizeArrangementRole(
            PurchaseResourceEntity resource,
            String requestedRole,
            String existingRole
    ) {
        if ("ground_agent".equals(resource.getResourceType())) {
            throw new BizException("地接服务请在真实团队安排阶段配置");
        }
        if ("vehicle".equals(resource.getResourceType()) || "traffic".equals(resource.getResourceType())) {
            throw new BizException("用车和交通不能安排到某一天，请使用产品级安排");
        }
        String role = clean(requestedRole);
        if (!StringUtils.hasText(role)) {
            if (StringUtils.hasText(existingRole)) {
                role = existingRole;
            } else if ("hotel".equals(resource.getResourceType())) {
                role = ARRANGEMENT_ACCOMMODATION;
            } else if ("restaurant".equals(resource.getResourceType())) {
                throw new BizException("餐厅资源必须选择早餐、中餐或晚餐");
            } else if ("ground_agent".equals(resource.getResourceType())) {
                role = ARRANGEMENT_GROUND_SERVICE;
            } else {
                role = ARRANGEMENT_ITINERARY;
            }
        }
        ProductDesignerArrangementRole resolvedRole = ProductDesignerArrangementRole.fromValue(role);
        if (resolvedRole == ProductDesignerArrangementRole.LEGACY_UNASSIGNED
                && (!StringUtils.hasText(existingRole) || StringUtils.hasText(requestedRole))) {
            throw new BizException("餐厅资源必须选择早餐、中餐或晚餐");
        }
        if (ARRANGEMENT_ACCOMMODATION.equals(role) && !"hotel".equals(resource.getResourceType())) {
            throw new BizException("住宿位置只能安排酒店资源");
        }
        if ((MEAL_ARRANGEMENT_ROLES.contains(role) || ARRANGEMENT_UNASSIGNED.equals(role))
                && !"restaurant".equals(resource.getResourceType())) {
            throw new BizException("餐次位置只能安排餐厅资源");
        }
        if (ARRANGEMENT_GROUND_SERVICE.equals(role) && !"ground_agent".equals(resource.getResourceType())) {
            throw new BizException("地接服务位置只能安排地接资源");
        }
        if (ARRANGEMENT_ITINERARY.equals(role)
                && ("hotel".equals(resource.getResourceType()) || "restaurant".equals(resource.getResourceType())
                || "ground_agent".equals(resource.getResourceType()))) {
            throw new BizException("酒店、餐厅和地接必须安排到对应区域");
        }
        return role;
    }

    /** 外部早餐与前一晚酒店含早二选一，替换操作必须在同一事务内完成。 */
    private void assertOrClearPreviousNightBreakfastHotels(
            Long tenantId, Long productId, Integer dayNo, Boolean replaceBreakfastSource, String operator
    ) {
        if (dayNo == null || dayNo <= 1) {
            return;
        }
        List<SalesProductDayResourceEntity> hotels = dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId)
                .eq("day_no", dayNo - 1)
                .eq("arrangement_role", ARRANGEMENT_ACCOMMODATION)
                .eq("hotel_breakfast_included", true));
        if (hotels.isEmpty()) {
            return;
        }
        if (!Boolean.TRUE.equals(replaceBreakfastSource)) {
            throw new BizException("前一晚酒店已包含次日早餐，如需改为外部早餐请确认替换");
        }
        dayResourceMapper.update(null, baseDayResourceUpdate(tenantId, productId)
                .eq("day_no", dayNo - 1)
                .eq("arrangement_role", ARRANGEMENT_ACCOMMODATION)
                .eq("hotel_breakfast_included", true)
                .set("hotel_breakfast_included", false)
                .set("updated_by", operator));
    }

    /** 勾选酒店含早时，需要先处理次日已经存在的外部早餐餐厅。 */
    private void assertOrClearNextDayBreakfastRestaurant(
            Long tenantId, Long productId, Integer nextDayNo, Boolean replaceBreakfastSource, String operator
    ) {
        List<SalesProductDayResourceEntity> breakfasts = dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId)
                .eq("day_no", nextDayNo)
                .eq("arrangement_role", ARRANGEMENT_BREAKFAST));
        if (breakfasts.isEmpty()) {
            return;
        }
        if (!Boolean.TRUE.equals(replaceBreakfastSource)) {
            throw new BizException("次日已安排外部早餐，如需改为酒店含早请确认替换");
        }
        OffsetDateTime now = OffsetDateTime.now();
        dayResourceMapper.update(null, baseDayResourceUpdate(tenantId, productId)
                .eq("day_no", nextDayNo)
                .eq("arrangement_role", ARRANGEMENT_BREAKFAST)
                .set("is_deleted", true)
                .set("deleted_at", now)
                .set("deleted_by", operator));
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
            // 产品设计阶段允许先放入尚未询价的资源，成本暂按 0 处理；明确选择了供应商时仍必须校验绑定关系。
            if (supplierId == null) {
                return null;
            }
            throw new BizException("供应商未绑定当前资源或已停用");
        }
        return relation;
    }

    /**
     * 资源加入和更换供应商共用的报价入口。
     *
     * <p>生产注入统一解析器后可精确锁定采购关系；构造型遗留单测仍保留原查询逻辑作为兼容兜底。</p>
     */
    private ProductDesignerSupplierQuote resolveSupplierQuote(
            Long tenantId, PurchaseResourceEntity resource, Long supplierRelationId, Long supplierId
    ) {
        if (supplierQuoteService != null) {
            return supplierQuoteService.resolve(tenantId, resource, supplierRelationId, supplierId);
        }
        if (supplierRelationId != null) {
            throw new BizException("供应商采购关系服务未配置");
        }
        PurchaseRelationEntity relation = relationForSave(tenantId, resource, supplierId);
        if (relation == null) {
            return PurchaseResourceProcurementMode.NOT_REQUIRED.value().equals(resource.getProcurementMode())
                    ? ProductDesignerSupplierQuote.notRequired()
                    : ProductDesignerSupplierQuote.pendingQuote();
        }
        return new ProductDesignerSupplierQuote(
                relation.getId(), relation.getSupplierId(), supplierName(tenantId, relation.getSupplierId()),
                relation.getPriceMode(), unitPriceForSave(tenantId, resource, relation), false
        );
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

    private List<Long> normalizedIntroductionIds(ProductDesignerDayResourceSaveRequest request) {
        if (request.introductionIds() != null) {
            return request.introductionIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return request.selectedIntroductionId() == null
                ? List.of()
                : List.of(request.selectedIntroductionId());
    }

    /** 将统一素材数组拆为普通介绍，并校验其确实不是自费项目素材。 */
    private List<PurchaseResourceIntroductionEntity> standardIntroductionsForMaterials(
            Long tenantId,
            Long resourceId,
            List<ProductDesignerSelectedMaterialRequest> materials
    ) {
        List<Long> ids = materials.stream()
                .filter(item -> "introduction".equals(item.materialType()))
                .map(ProductDesignerSelectedMaterialRequest::introductionId)
                .toList();
        if (ids.stream().anyMatch(Objects::isNull) || materials.stream()
                .anyMatch(item -> !"introduction".equals(item.materialType())
                        && !"optional_item".equals(item.materialType()))) {
            throw new BizException("素材类型或介绍素材不正确");
        }
        if (ids.size() != new java.util.HashSet<>(ids).size()) {
            throw new BizException("普通介绍素材不能重复选择");
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<PurchaseResourceIntroductionEntity> found = introductionMapper.selectList(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .eq("status", INTRODUCTION_PUBLISHED)
                        .eq("is_optional_item", false)
                        .in("id", ids));
        Map<Long, PurchaseResourceIntroductionEntity> byId = found.stream()
                .collect(Collectors.toMap(PurchaseResourceIntroductionEntity::getId, Function.identity()));
        if (byId.size() != ids.size() || ids.stream().anyMatch(id -> !byId.containsKey(id))) {
            throw new BizException("只能选择当前资源已发布的普通介绍素材");
        }
        return ids.stream().map(byId::get).toList();
    }

    /** 把统一自费素材转换为原有快照服务的请求，并在落库前阻止重复项目。 */
    private List<ProductDesignerSelectedOptionalItemRequest> optionalMaterialRows(
            List<ProductDesignerSelectedMaterialRequest> materials
    ) {
        List<ProductDesignerSelectedOptionalItemRequest> rows = materials.stream()
                .filter(item -> "optional_item".equals(item.materialType()))
                .map(item -> {
                    if (item.resourceOptionalItemId() == null) {
                        throw new BizException("自费项目不能为空");
                    }
                    return new ProductDesignerSelectedOptionalItemRequest(
                            item.resourceOptionalItemId(), item.introductionId(), item.supplierOptionalItemId(),
                            null, null, item.salePrice());
                })
                .toList();
        if (rows.stream().map(ProductDesignerSelectedOptionalItemRequest::resourceOptionalItemId)
                .collect(Collectors.toSet()).size() != rows.size()) {
            throw new BizException("自费项目不能重复选择");
        }
        return rows;
    }

    private List<Integer> standardMaterialSortOrders(List<ProductDesignerSelectedMaterialRequest> materials) {
        return java.util.stream.IntStream.range(0, materials.size())
                .filter(index -> "introduction".equals(materials.get(index).materialType()))
                .map(index -> index + 1).boxed().toList();
    }

    private List<Integer> optionalMaterialSortOrders(List<ProductDesignerSelectedMaterialRequest> materials) {
        return java.util.stream.IntStream.range(0, materials.size())
                .filter(index -> "optional_item".equals(materials.get(index).materialType()))
                .map(index -> index + 1).boxed().toList();
    }

    private List<Long> normalizedIntroductionIds(ProductDesignerIntroductionSaveRequest request) {
        if (request.introductionIds() != null) {
            return request.introductionIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return request.selectedIntroductionId() == null
                ? List.of()
                : List.of(request.selectedIntroductionId());
    }

    /**
     * 按请求顺序批量读取并校验当前资源的已发布介绍素材。
     */
    private List<PurchaseResourceIntroductionEntity> introductionsForSave(
            Long tenantId,
            Long resourceId,
            ProductDesignerDayResourceSaveRequest request,
            List<Long> introductionIds
    ) {
        if (request.introductionIds() == null && request.selectedIntroductionId() != null) {
            return List.of(introductionForSave(tenantId, resourceId, request.selectedIntroductionId()));
        }
        return introductionsForSaveByIds(tenantId, resourceId, introductionIds);
    }

    private List<PurchaseResourceIntroductionEntity> introductionsForSave(
            Long tenantId,
            Long resourceId,
            ProductDesignerIntroductionSaveRequest request,
            List<Long> introductionIds
    ) {
        if (request.introductionIds() == null && request.selectedIntroductionId() != null) {
            return List.of(introductionForSave(tenantId, resourceId, request.selectedIntroductionId()));
        }
        return introductionsForSaveByIds(tenantId, resourceId, introductionIds);
    }

    private List<PurchaseResourceIntroductionEntity> introductionsForSaveByIds(
            Long tenantId,
            Long resourceId,
            List<Long> introductionIds
    ) {
        if (introductionIds == null || introductionIds.isEmpty()) {
            return List.of();
        }
        List<PurchaseResourceIntroductionEntity> introductions = introductionMapper.selectList(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .eq("status", INTRODUCTION_PUBLISHED)
                        .in("id", introductionIds)
        );
        Map<Long, PurchaseResourceIntroductionEntity> byId = introductions.stream()
                .collect(Collectors.toMap(PurchaseResourceIntroductionEntity::getId, Function.identity()));
        if (byId.size() != introductionIds.size()
                || introductionIds.stream().anyMatch(id -> !byId.containsKey(id))) {
            throw new BizException("只能选择当前资源已发布的介绍版本");
        }
        return introductionIds.stream().map(byId::get).toList();
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

    /** 批量读取已发布介绍绑定的有效资源图片，确保产品详情接口不产生 N+1 查询。 */
    private Map<Long, List<Long>> introductionImageIdsByIntroduction(
            Long tenantId,
            List<PurchaseResourceIntroductionEntity> introductions,
            List<PurchaseResourceImageEntity> activeResourceImages
    ) {
        Map<Long, List<PurchaseResourceImageEntity>> imagesByResource = activeResourceImages.stream()
                .collect(Collectors.groupingBy(
                        PurchaseResourceImageEntity::getResourceId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return introductionImageIdsByIntroduction(tenantId, introductions, imagesByResource);
    }

    /** 批量版本：按介绍所属资源过滤其有效图片关联。 */
    private Map<Long, List<Long>> introductionImageIdsByIntroduction(
            Long tenantId,
            List<PurchaseResourceIntroductionEntity> introductions,
            Map<Long, List<PurchaseResourceImageEntity>> activeImagesByResource
    ) {
        if (introductionImageMapper == null || introductions.isEmpty() || activeImagesByResource.isEmpty()) {
            return Map.of();
        }
        List<Long> introductionIds = introductions.stream()
                .map(PurchaseResourceIntroductionEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (introductionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<Long>> activeImageIdsByResource = activeImagesByResource.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(PurchaseResourceImageEntity::getId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (activeImageIdsByResource.values().stream().allMatch(Set::isEmpty)) {
            return Map.of();
        }
        List<PurchaseResourceIntroductionImageEntity> links = introductionImageMapper.selectList(
                new QueryWrapper<PurchaseResourceIntroductionImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("introduction_id", introductionIds)
                        .orderByAsc("introduction_id")
                        .orderByAsc("sort_order")
                        .orderByAsc("id")
        );
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> imageIdsByIntroduction = new LinkedHashMap<>();
        for (PurchaseResourceIntroductionImageEntity link : links) {
            PurchaseResourceIntroductionEntity introduction = introductions.stream()
                    .filter(item -> Objects.equals(item.getId(), link.getIntroductionId()))
                    .findFirst()
                    .orElse(null);
            Set<Long> activeImageIds = introduction == null
                    ? Set.of()
                    : activeImageIdsByResource.getOrDefault(introduction.getResourceId(), Set.of());
            if (activeImageIds.contains(link.getResourceImageId())) {
                imageIdsByIntroduction
                        .computeIfAbsent(link.getIntroductionId(), ignored -> new java.util.ArrayList<>())
                        .add(link.getResourceImageId());
            }
        }
        return imageIdsByIntroduction;
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
            entity.setIntroductionWarmTipSnapshot(null);
            entity.setIntroductionVisitDurationSnapshot(null);
            return;
        }
        entity.setSelectedIntroductionId(introduction.getId());
        entity.setIntroductionIndexVersion(introduction.getIndexVersion());
        entity.setIntroductionTitleSnapshot(introduction.getTitle());
        entity.setIntroductionContentSnapshot(introduction.getContent());
        entity.setIntroductionNoticeSnapshot(clean(introduction.getNoticeContent()));
        entity.setIntroductionWarmTipSnapshot(clean(introduction.getWarmTipContent()));
        entity.setIntroductionVisitDurationSnapshot(clean(introduction.getVisitDuration()));
    }

    private void applyIntroductionSnapshot(
            SalesProductDayResourceEntity entity,
            List<PurchaseResourceIntroductionEntity> introductions
    ) {
        PurchaseResourceIntroductionEntity first = introductions == null || introductions.isEmpty()
                ? null
                : introductions.getFirst();
        applyIntroductionSnapshot(entity, first);
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

    private void applyIntroductionSnapshotForSave(
            SalesProductDayResourceEntity entity,
            Long resourceId,
            List<PurchaseResourceIntroductionEntity> introductions
    ) {
        applyIntroductionSnapshot(entity, introductions);
    }

    private void saveIntroductionSnapshots(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            List<PurchaseResourceIntroductionEntity> introductions,
            String operator
    ) {
        saveIntroductionSnapshots(tenantId, dayResource, introductions,
                java.util.stream.IntStream.rangeClosed(1, introductions == null ? 0 : introductions.size())
                        .boxed().toList(), operator);
    }

    /** 保存普通介绍快照；统一素材契约传入的排序是普通、自费混排后的绝对序号。 */
    private void saveIntroductionSnapshots(
            Long tenantId,
            SalesProductDayResourceEntity dayResource,
            List<PurchaseResourceIntroductionEntity> introductions,
            List<Integer> sortOrders,
            String operator
    ) {
        SalesProductDayResourceIntroductionEntity deleted =
                new SalesProductDayResourceIntroductionEntity();
        markDeleted(deleted, operator, OffsetDateTime.now());
        dayResourceIntroductionMapper.update(deleted,
                new UpdateWrapper<SalesProductDayResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", dayResource.getProductId())
                        .eq("day_resource_id", dayResource.getId()));
        if (introductions == null || introductions.isEmpty()) {
            return;
        }
        if (sortOrders == null || sortOrders.size() != introductions.size()) {
            throw new BizException("介绍素材排序数据不完整");
        }
        for (int index = 0; index < introductions.size(); index += 1) {
            PurchaseResourceIntroductionEntity introduction = introductions.get(index);
            SalesProductDayResourceIntroductionEntity snapshot =
                    new SalesProductDayResourceIntroductionEntity();
            snapshot.setTenantId(tenantId);
            snapshot.setProductId(dayResource.getProductId());
            snapshot.setDayResourceId(dayResource.getId());
            snapshot.setResourceIntroductionId(introduction.getId());
            snapshot.setIntroductionIndexVersion(introduction.getIndexVersion());
            snapshot.setTitleSnapshot(introduction.getTitle());
            snapshot.setContentSnapshot(introduction.getContent());
            snapshot.setNoticeSnapshot(clean(introduction.getNoticeContent()));
            snapshot.setWarmTipSnapshot(clean(introduction.getWarmTipContent()));
            snapshot.setExtensionBlocksSnapshot(introduction.getExtensionBlocks());
            snapshot.setVisitDurationSnapshot(clean(introduction.getVisitDuration()));
            snapshot.setSortOrder(sortOrders.get(index));
            snapshot.setCreatedBy(operator);
            snapshot.setIsDeleted(false);
            dayResourceIntroductionMapper.insert(snapshot);
        }
    }

    /** 未接入扩展模块的历史快照按空数组回显，避免影响既有产品。 */
    private List<com.mtravel.platform.purchase.resource.material.dto.ResourceIntroductionExtensionBlock> extensionBlocks(String value) {
        return extensionBlockCodec == null ? List.of() : extensionBlockCodec.decode(value);
    }

    private BigDecimal unitPriceForSave(Long tenantId, PurchaseResourceEntity resource, PurchaseRelationEntity relation) {
        if (PurchaseResourceProcurementMode.NOT_REQUIRED.value().equals(resource.getProcurementMode())) {
            return BigDecimal.ZERO;
        }
        return referenceUnitPrice(tenantId, relation);
    }

    private List<ProductDesignerSupplierResponse> supplierResponses(Long tenantId, PurchaseResourceEntity resource) {
        if ("vehicle".equals(resource.getResourceType())) {
            List<PurchaseRelationSupplierPriceRow> rows = relationMapper
                    .selectActiveResourceSupplierRows(tenantId, List.of(resource.getId()));
            return rows == null || rows.isEmpty() ? List.of() : supplierResponses(rows, Map.of());
        }
        return supplierResponsesByResource(tenantId, List.of(resource.getId()))
                .getOrDefault(resource.getId(), List.of());
    }

    /** 批量读取景区资源自费项目主档，供当天方案左侧候选列表使用。 */
    private Map<Long, List<PurchaseResourceOptionalItemResponse>> optionalItemsByResource(
            Long tenantId,
            List<Long> resourceIds
    ) {
        if (resourceOptionalItemMapper == null || resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return resourceOptionalItemMapper.selectList(new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("resource_id", resourceIds)
                        .orderByAsc("resource_id")
                        .orderByAsc("project_name")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        PurchaseResourceOptionalItemEntity::getResourceId,
                        LinkedHashMap::new,
                        Collectors.mapping(PurchaseResourceOptionalItemResponse::fromEntity, Collectors.toList())
                ));
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
        List<Long> relationIds = rows.stream().map(PurchaseRelationSupplierPriceRow::getRelationId).distinct().toList();
        Map<Long, List<ProductDesignerSupplierOptionalItemResponse>> optionalByRelation = relationOptionalItemMapper == null ? Map.of() : relationOptionalItemMapper.selectList(new QueryWrapper<PurchaseRelationOptionalItemEntity>().eq("tenant_id", tenantId).eq("is_deleted", false).eq("status", STATUS_ACTIVE).in("relation_id", relationIds)).stream().collect(Collectors.groupingBy(PurchaseRelationOptionalItemEntity::getRelationId, LinkedHashMap::new, Collectors.mapping(item -> new ProductDesignerSupplierOptionalItemResponse(item.getId(), item.getResourceOptionalItemId(), item.getProjectName(), money(item.getCostPrice()), money(item.getSuggestedSalePrice()), item.getStatus()), Collectors.toList())));
        byResource.forEach((resourceId, resourceRows) -> result.put(resourceId, supplierResponses(resourceRows, optionalByRelation)));
        return result;
    }

    private List<ProductDesignerSupplierResponse> supplierResponses(List<PurchaseRelationSupplierPriceRow> rows, Map<Long, List<ProductDesignerSupplierOptionalItemResponse>> optionalByRelation) {
        return rows.stream()
                .collect(Collectors.groupingBy(
                        PurchaseRelationSupplierPriceRow::getRelationId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(group -> supplierResponse(group, optionalByRelation.getOrDefault(group.getFirst().getRelationId(), List.of())))
                .toList();
    }

    private ProductDesignerSupplierResponse supplierResponse(List<PurchaseRelationSupplierPriceRow> rows, List<ProductDesignerSupplierOptionalItemResponse> optionalItems) {
        PurchaseRelationSupplierPriceRow relation = rows.getFirst();
        List<ProductDesignerSupplierPriceLineResponse> lines = rows.stream()
                .filter(item -> item.getResourceProjectId() != null)
                .map(item -> new ProductDesignerSupplierPriceLineResponse(
                        item.getResourceProjectId(), item.getProjectName(), money(item.getMarketPrice()),
                        money(item.getPeerPrice()), money(item.getTeamPrice())
                ))
                .toList();
        BigDecimal referencePrice;
        boolean hasLinePrice = PRICE_MODE_UNIFIED.equals(relation.getPriceMode())
                ? relation.getUnifiedPrice() != null
                : rows.stream().map(this::bestLinePrice).filter(Objects::nonNull).findFirst().isPresent();
        if (hasLinePrice) {
            referencePrice = PRICE_MODE_UNIFIED.equals(relation.getPriceMode())
                    ? money(relation.getUnifiedPrice())
                    : rows.stream().map(this::bestLinePrice).filter(Objects::nonNull).findFirst().orElse(BigDecimal.ZERO);
        } else {
            referencePrice = BigDecimal.ZERO;
        }
        String priceMode = hasLinePrice ? relation.getPriceMode() : "pending";
        return new ProductDesignerSupplierResponse(
                relation.getRelationId(),
                relation.getSupplierId(),
                relation.getSupplierName(),
                Boolean.TRUE.equals(relation.getDefaultSupplier()),
                priceMode,
                money(relation.getUnifiedPrice()),
                referencePrice,
                lines,
                optionalItems
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

    private int nextSortOrder(Long tenantId, Long productId, Integer dayNo, String arrangementRole) {
        List<SalesProductDayResourceEntity> current = dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId)
                .eq("day_no", dayNo)
                .eq("arrangement_role", arrangementRole)
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
        BigDecimal dayResourceCost = dayResourceMapper.selectList(baseDayResourceQuery(tenantId, productId))
                .stream()
                .filter(item -> !ARRANGEMENT_GROUND_SERVICE.equals(item.getArrangementRole()))
                .map(SalesProductDayResourceEntity::getCostAmountSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vehicleCost = vehicleArrangementService == null
                ? BigDecimal.ZERO
                : vehicleArrangementService.costAmount(tenantId, productId);
        return dayResourceCost.add(vehicleCost).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
