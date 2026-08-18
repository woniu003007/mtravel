package com.mtravel.platform.purchase.resource.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceBindingResponse;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceResponse;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceSaveRequest;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierPriceLineResponse;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceProcurementMode;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceStatus;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceType;
import com.mtravel.platform.purchase.resource.enums.ScenicBusinessStatus;
import com.mtravel.platform.purchase.resource.enums.ScenicLevel;
import com.mtravel.platform.purchase.resource.enums.ScenicSiteVisitStatus;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> PLACE_RESOURCE_TYPES = List.of("scenic", "hotel", "restaurant", "shopping");

    private final PurchaseResourceMapper resourceMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseRelationMapper relationMapper;
    private final SupplierResourcePriceMapper priceMapper;

    @Autowired
    public PurchaseResourceService(
            PurchaseResourceMapper resourceMapper,
            SupplierMapper supplierMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper
    ) {
        super(resourceMapper);
        this.resourceMapper = resourceMapper;
        this.supplierMapper = supplierMapper;
        this.relationMapper = relationMapper;
        this.priceMapper = priceMapper;
    }

    /** 保留无报价映射器的单元测试及历史构造入口兼容性。 */
    public PurchaseResourceService(
            PurchaseResourceMapper resourceMapper,
            SupplierMapper supplierMapper,
            PurchaseRelationMapper relationMapper
    ) {
        this(resourceMapper, supplierMapper, relationMapper, null);
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
            String procurementMode,
            String scenicLevel,
            String businessStatus,
            String siteVisitStatus,
            long page,
            long pageSize
    ) {
        validateFilters(procurementMode, scenicLevel, businessStatus, siteVisitStatus);
        QueryWrapper<PurchaseResourceEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                .eq(StringUtils.hasText(province), "province", province)
                .eq(StringUtils.hasText(city), "city", city)
                .eq(StringUtils.hasText(district), "district", district)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(StringUtils.hasText(procurementMode), "procurement_mode", procurementMode)
                .eq(StringUtils.hasText(scenicLevel), "scenic_level", scenicLevel)
                .eq(StringUtils.hasText(businessStatus), "business_status", businessStatus)
                .eq(StringUtils.hasText(siteVisitStatus), "site_visit_status", siteVisitStatus)
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

        if (Boolean.TRUE.equals(request.autoCreateSupplier())
                && PurchaseResourceProcurementMode.NOT_REQUIRED.value().equals(entity.getProcurementMode())) {
            throw new BizException("无需采购的资源不能自动创建供应商");
        }
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

    /**
     * 软删除资源时只更新删除标记字段。
     *
     * <p>资源实体里景区等级、地图坐标等类型专属字段使用 ALWAYS 更新策略，用空实体软删除会把这些字段
     * 一并写成空值，从而触发数据库类型联动约束。这里用 UpdateWrapper 明确 set 删除字段，避免误改业务字段。</p>
     */
    @Override
    public void delete(Long id, Long tenantId, String operator) {
        UpdateWrapper<PurchaseResourceEntity> wrapper = baseUpdate(tenantId)
                .eq("id", id)
                .set("is_deleted", true)
                .set("deleted_at", OffsetDateTime.now())
                .set("deleted_by", operator);
        int updated = resourceMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
    }

    /** 查询某个资源已经绑定的供应商列表。 */
    public List<PurchaseResourceBindingResponse> bindings(Long resourceId, Long tenantId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(baseQuery(tenantId).eq("id", resourceId));
        if (resource == null) {
            throw new BizException(notFoundMessage());
        }
        List<PurchaseRelationEntity> relations = relationMapper.selectList(new QueryWrapper<PurchaseRelationEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_type", resource.getResourceType())
                        .eq("resource_id", resource.getId())
                        .orderByAsc("group_quantity")
                        .orderByDesc("id"));
        if (relations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ResourceSupplierPriceLineResponse>> pricesByRelation = priceMapper == null
                ? Map.of()
                : priceMapper.selectList(
                        new QueryWrapper<SupplierResourcePriceEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("is_deleted", false)
                                .in("relation_id", relations.stream().map(PurchaseRelationEntity::getId).toList())
                                .orderByAsc("resource_project_id")
                                .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        SupplierResourcePriceEntity::getRelationId,
                        Collectors.mapping(ResourceSupplierPriceLineResponse::fromEntity, Collectors.toList())
                ));
        Map<Long, String> supplierNames = supplierNames(tenantId, relations);
        return relations
                .stream()
                .map(item -> PurchaseResourceBindingResponse.fromEntity(
                        item,
                        supplierNames.get(item.getSupplierId()),
                        pricesByRelation.getOrDefault(item.getId(), List.of())
                ))
                .toList();
    }

    /** 将接口字段清洗后写入实体，防止空字符串进入基础资料表。 */
    private void applyFields(PurchaseResourceEntity entity, PurchaseResourceSaveRequest request) {
        String resourceType = cleanRequired(request.resourceType());
        if (!PurchaseResourceType.contains(resourceType)) {
            throw new BizException("资源类型不合法");
        }
        entity.setResourceType(resourceType);
        entity.setProcurementMode(procurementMode(request.procurementMode()));
        entity.setResourceName(cleanRequired(request.resourceName()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setPhone(clean(request.phone()));
        entity.setContactName(clean(request.contactName()));
        entity.setFax(clean(request.fax()));
        entity.setAddress(clean(request.address()));
        applyTypedFields(entity, request, resourceType);
        entity.setWarmTip(request.warmTip());
        entity.setIntroduction(request.introduction());
        entity.setStatus(StringUtils.hasText(request.status())
                ? request.status()
                : PurchaseResourceStatus.ACTIVE.value());
        entity.setRemark(request.remark());
    }

    /**
     * 写入并校验各资源类型字段。
     *
     * <p>地点类资源共享地图、营业和踩点字段；非地点类资源保存时主动清空这些字段。
     * 配合实体字段的 ALWAYS 更新策略，类型切换不会残留旧类型专属数据。</p>
     */
    private void applyTypedFields(
            PurchaseResourceEntity entity,
            PurchaseResourceSaveRequest request,
            String resourceType
    ) {
        applyScenicLevel(entity, request, resourceType);
        applyStarLevel(entity, request, resourceType);
        entity.setCategoryTags(categoryTagsForType(request, resourceType));
        if (PLACE_RESOURCE_TYPES.contains(resourceType)) {
            applyPlaceFields(entity, request);
        } else {
            clearPlaceFields(entity);
        }
        entity.setCapacity(capacityForType(request, resourceType));
        entity.setTableCount(PurchaseResourceType.RESTAURANT.value().equals(resourceType) ? request.tableCount() : null);
        entity.setMealStandard(PurchaseResourceType.RESTAURANT.value().equals(resourceType) ? clean(request.mealStandard()) : null);
        entity.setVehicleType(PurchaseResourceType.VEHICLE.value().equals(resourceType) ? clean(request.vehicleType()) : null);
        entity.setSeatCount(PurchaseResourceType.VEHICLE.value().equals(resourceType) ? request.seatCount() : null);
        entity.setBillingMode(PurchaseResourceType.VEHICLE.value().equals(resourceType) ? clean(request.billingMode()) : null);
        entity.setServiceArea(serviceAreaForType(request, resourceType));
        entity.setReferenceDays(PurchaseResourceType.GROUND_AGENT.value().equals(resourceType) ? request.referenceDays() : null);
        entity.setIncludedItems(includedItemsForType(request, resourceType));
        entity.setExcludedItems(PurchaseResourceType.GROUND_AGENT.value().equals(resourceType) ? clean(request.excludedItems()) : null);
        entity.setResourceUnit(PurchaseResourceType.OTHER.value().equals(resourceType) ? clean(request.resourceUnit()) : null);
        validateTypedRequiredFields(entity, resourceType);
    }

    /** 景区等级只对景区生效，其它类型不能残留。 */
    private void applyScenicLevel(PurchaseResourceEntity entity, PurchaseResourceSaveRequest request, String resourceType) {
        if (!PurchaseResourceType.SCENIC.value().equals(resourceType)) {
            entity.setScenicLevel(null);
            return;
        }
        String scenicLevel = StringUtils.hasText(request.scenicLevel()) ? request.scenicLevel() : ScenicLevel.UNRATED.value();
        if (!ScenicLevel.contains(scenicLevel)) {
            throw new BizException("景区等级不合法");
        }
        entity.setScenicLevel(scenicLevel);
    }

    /** 酒店和餐厅可维护星级，其它资源不保存星级字段。 */
    private void applyStarLevel(PurchaseResourceEntity entity, PurchaseResourceSaveRequest request, String resourceType) {
        if (!PurchaseResourceType.HOTEL.value().equals(resourceType)
                && !PurchaseResourceType.RESTAURANT.value().equals(resourceType)) {
            entity.setStarLevel(null);
            return;
        }
        entity.setStarLevel(StringUtils.hasText(request.starLevel()) ? request.starLevel() : "unrated");
    }

    /** 地点类资源共享定位、营业和踩点字段。 */
    private void applyPlaceFields(PurchaseResourceEntity entity, PurchaseResourceSaveRequest request) {
        String businessStatus = StringUtils.hasText(request.businessStatus())
                ? request.businessStatus()
                : ScenicBusinessStatus.UNMAINTAINED.value();
        if (!ScenicBusinessStatus.contains(businessStatus)) {
            throw new BizException("营业状态不合法");
        }
        String siteVisitStatus = StringUtils.hasText(request.siteVisitStatus())
                ? request.siteVisitStatus()
                : ScenicSiteVisitStatus.UNMAINTAINED.value();
        if (!ScenicSiteVisitStatus.contains(siteVisitStatus)) {
            throw new BizException("踩点状态不合法");
        }

        validateCoordinates(request.longitude(), request.latitude());
        validateBusinessHours(request.openingTime(), request.closingTime());
        LocalDate lastSiteVisitDate = validatedSiteVisitDate(siteVisitStatus, request.lastSiteVisitDate());

        entity.setLongitude(request.longitude());
        entity.setLatitude(request.latitude());
        entity.setBusinessStatus(businessStatus);
        entity.setOpeningTime(request.openingTime());
        entity.setClosingTime(request.closingTime());
        entity.setSiteVisitStatus(siteVisitStatus);
        entity.setLastSiteVisitDate(lastSiteVisitDate);
        entity.setSiteVisitNote(clean(request.siteVisitNote()));
    }

    /** 非地点类资源不能残留定位、营业或踩点信息。 */
    private void clearPlaceFields(PurchaseResourceEntity entity) {
        entity.setLongitude(null);
        entity.setLatitude(null);
        entity.setBusinessStatus(null);
        entity.setOpeningTime(null);
        entity.setClosingTime(null);
        entity.setSiteVisitStatus(null);
        entity.setLastSiteVisitDate(null);
        entity.setSiteVisitNote(null);
    }

    private String categoryTagsForType(PurchaseResourceSaveRequest request, String resourceType) {
        if (PurchaseResourceType.RESTAURANT.value().equals(resourceType)
                || PurchaseResourceType.SHOPPING.value().equals(resourceType)
                || PurchaseResourceType.TRAFFIC.value().equals(resourceType)
                || PurchaseResourceType.OTHER.value().equals(resourceType)) {
            return clean(request.categoryTags());
        }
        return null;
    }

    private Integer capacityForType(PurchaseResourceSaveRequest request, String resourceType) {
        if (PurchaseResourceType.RESTAURANT.value().equals(resourceType)
                || PurchaseResourceType.SHOPPING.value().equals(resourceType)) {
            return request.capacity();
        }
        return null;
    }

    private String serviceAreaForType(PurchaseResourceSaveRequest request, String resourceType) {
        if (PurchaseResourceType.TRAFFIC.value().equals(resourceType)
                || PurchaseResourceType.GROUND_AGENT.value().equals(resourceType)) {
            return clean(request.serviceArea());
        }
        return null;
    }

    private String includedItemsForType(PurchaseResourceSaveRequest request, String resourceType) {
        if (PurchaseResourceType.GROUND_AGENT.value().equals(resourceType)
                || PurchaseResourceType.TRAFFIC.value().equals(resourceType)
                || PurchaseResourceType.OTHER.value().equals(resourceType)) {
            return clean(request.includedItems());
        }
        return null;
    }

    /** 校验当前资源类型必须具备的核心字段。 */
    private void validateTypedRequiredFields(PurchaseResourceEntity entity, String resourceType) {
        if (PurchaseResourceType.VEHICLE.value().equals(resourceType) && entity.getSeatCount() == null) {
            throw new BizException("用车资源必须填写座位数");
        }
        if (PurchaseResourceType.GROUND_AGENT.value().equals(resourceType) && !StringUtils.hasText(entity.getServiceArea())) {
            throw new BizException("地接资源必须填写服务地区");
        }
    }

    /** 经纬度只能成对填写，且必须位于合法地理范围内。 */
    private void validateCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if ((longitude == null) != (latitude == null)) {
            throw new BizException("经度和纬度必须同时填写");
        }
        if (longitude == null) {
            return;
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BizException("经度必须在-180到180之间");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BizException("纬度必须在-90到90之间");
        }
    }

    /** 营业时间必须成对填写，并按当前固定营业时段口径限制在同一天内。 */
    private void validateBusinessHours(LocalTime openingTime, LocalTime closingTime) {
        if ((openingTime == null) != (closingTime == null)) {
            throw new BizException("开始营业时间和结束营业时间必须同时填写");
        }
        if (openingTime != null && !closingTime.isAfter(openingTime)) {
            throw new BizException("结束营业时间必须晚于开始营业时间");
        }
    }

    /** 已踩点必须记录不晚于当天的日期，其它状态不保留历史日期。 */
    private LocalDate validatedSiteVisitDate(String siteVisitStatus, LocalDate lastSiteVisitDate) {
        if (!ScenicSiteVisitStatus.VISITED.value().equals(siteVisitStatus)) {
            return null;
        }
        if (lastSiteVisitDate == null) {
            throw new BizException("已踩点时必须填写最近踩点日期");
        }
        if (lastSiteVisitDate.isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new BizException("最近踩点日期不能晚于当天");
        }
        return lastSiteVisitDate;
    }

    /** 列表筛选值同样由后端校验，避免任意字符串直接进入查询条件。 */
    private void validateFilters(
            String procurementMode, String scenicLevel, String businessStatus, String siteVisitStatus
    ) {
        if (StringUtils.hasText(procurementMode) && !PurchaseResourceProcurementMode.contains(procurementMode)) {
            throw new BizException("默认采购属性筛选值不合法");
        }
        if (StringUtils.hasText(scenicLevel) && !ScenicLevel.contains(scenicLevel)) {
            throw new BizException("景区等级筛选值不合法");
        }
        if (StringUtils.hasText(businessStatus) && !ScenicBusinessStatus.contains(businessStatus)) {
            throw new BizException("营业状态筛选值不合法");
        }
        if (StringUtils.hasText(siteVisitStatus) && !ScenicSiteVisitStatus.contains(siteVisitStatus)) {
            throw new BizException("踩点状态筛选值不合法");
        }
    }

    /** 未明确选择时默认需要采购，避免将历史资源误判为免费。 */
    private String procurementMode(String value) {
        String procurementMode = StringUtils.hasText(value)
                ? value
                : PurchaseResourceProcurementMode.REQUIRED.value();
        if (!PurchaseResourceProcurementMode.contains(procurementMode)) {
            throw new BizException("默认采购属性不合法");
        }
        return procurementMode;
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
        supplier.setContactName(resource.getContactName());
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
        relation.setIsDefault(false);
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

    /** 批量查询绑定列表中的供应商名称，避免每条绑定单独访问数据库。 */
    private Map<Long, String> supplierNames(Long tenantId, List<PurchaseRelationEntity> relations) {
        List<Long> supplierIds = relations.stream()
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
                .collect(Collectors.toMap(SupplierEntity::getId, SupplierEntity::getSupplierName, (left, right) -> left));
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
