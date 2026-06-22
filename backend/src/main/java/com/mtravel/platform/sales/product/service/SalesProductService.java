package com.mtravel.platform.sales.product.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementItemRequest;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementItemResponse;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementPriceLineRequest;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementPriceLineResponse;
import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayRequest;
import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayResponse;
import com.mtravel.platform.sales.product.dto.SalesProductRoadbookPointRequest;
import com.mtravel.platform.sales.product.dto.SalesProductRoadbookPointResponse;
import com.mtravel.platform.sales.product.dto.SalesProductResponse;
import com.mtravel.platform.sales.product.dto.SalesProductSaveRequest;
import com.mtravel.platform.sales.product.dto.SalesProductVehicleInquiryRequest;
import com.mtravel.platform.sales.product.dto.SalesProductVehicleInquiryResponse;
import com.mtravel.platform.sales.product.dto.SalesProductVehicleQuoteSnapshotRequest;
import com.mtravel.platform.sales.product.dto.SalesProductVehicleQuoteSnapshotResponse;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementPriceLineEntity;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.entity.SalesProductRoadbookPointEntity;
import com.mtravel.platform.sales.product.entity.SalesProductVehicleInquiryEntity;
import com.mtravel.platform.sales.product.entity.SalesProductVehicleQuoteSnapshotEntity;
import com.mtravel.platform.sales.product.enums.SalesProductArrangementType;
import com.mtravel.platform.sales.product.enums.SalesProductDomesticType;
import com.mtravel.platform.sales.product.enums.SalesProductSettlementType;
import com.mtravel.platform.sales.product.enums.SalesProductStatus;
import com.mtravel.platform.sales.product.enums.SalesProductTripType;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementItemMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementPriceLineMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductRoadbookPointMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductVehicleInquiryMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductVehicleQuoteSnapshotMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 销售产品模板业务服务。
 *
 * <p>产品管理按线路模板处理。服务层集中维护租户隔离、产品名称唯一、状态枚举、行程天数校验、
 * 子表软删除重建和详情组装，Controller 不直接处理业务规则。</p>
 */
@Service
public class SalesProductService extends BusinessCrudService<SalesProductEntity, SalesProductResponse> {

    private final SalesProductMapper productMapper;
    private final SalesProductItineraryDayMapper itineraryMapper;
    private final SalesProductDescriptionMapper descriptionMapper;
    private final SalesProductArrangementItemMapper arrangementMapper;
    private final SalesProductArrangementPriceLineMapper priceLineMapper;
    private final SalesProductRoadbookPointMapper roadbookMapper;
    private final SalesProductVehicleQuoteSnapshotMapper vehicleQuoteSnapshotMapper;
    private final SalesProductVehicleInquiryMapper vehicleInquiryMapper;

    public SalesProductService(
            SalesProductMapper productMapper,
            SalesProductItineraryDayMapper itineraryMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductArrangementItemMapper arrangementMapper,
            SalesProductArrangementPriceLineMapper priceLineMapper,
            SalesProductRoadbookPointMapper roadbookMapper,
            SalesProductVehicleQuoteSnapshotMapper vehicleQuoteSnapshotMapper,
            SalesProductVehicleInquiryMapper vehicleInquiryMapper
    ) {
        super(productMapper);
        this.productMapper = productMapper;
        this.itineraryMapper = itineraryMapper;
        this.descriptionMapper = descriptionMapper;
        this.arrangementMapper = arrangementMapper;
        this.priceLineMapper = priceLineMapper;
        this.roadbookMapper = roadbookMapper;
        this.vehicleQuoteSnapshotMapper = vehicleQuoteSnapshotMapper;
        this.vehicleInquiryMapper = vehicleInquiryMapper;
    }

    /**
     * 分页查询销售产品模板。
     *
     * @param tenantId 当前租户ID
     * @param keyword 产品名称或城市关键字
     * @param businessType 业务类型筛选
     * @param receptionStandard 接待标准筛选
     * @param domesticInternational 国内国际筛选
     * @param status 产品状态筛选
     * @param page 当前页码
     * @param pageSize 每页数量
     * @return 产品分页结果
     */
    public PageResult<SalesProductResponse> page(
            Long tenantId,
            String keyword,
            String businessType,
            String receptionStandard,
            String domesticInternational,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesProductEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(businessType), "business_type", clean(businessType))
                .eq(StringUtils.hasText(receptionStandard), "reception_standard", clean(receptionStandard))
                .eq(StringUtils.hasText(domesticInternational), "domestic_international",
                        SalesProductDomesticType.fromValueOrDefault(domesticInternational).getValue())
                .eq(StringUtils.hasText(status), "status", SalesProductStatus.fromValueOrDefault(status).getValue())
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("product_name", keyword == null ? null : keyword.trim())
                        .or()
                        .like("city", keyword == null ? null : keyword.trim()))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询产品详情，包含行程内容、产品说明和团队安排参数。
     *
     * @param id 产品ID
     * @param tenantId 当前租户ID
     * @return 产品详情
     */
    @Override
    public SalesProductResponse detail(Long id, Long tenantId) {
        SalesProductEntity product = productMapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (product == null) {
            throw new BizException(notFoundMessage());
        }
        return detailResponse(product);
    }

    /**
     * 新增销售产品模板。
     *
     * <p>产品和子表必须在同一事务中保存，避免出现只有主表没有行程或说明的半成品模板。</p>
     *
     * @param request 保存请求
     * @param tenantId 当前租户ID
     * @param operator 当前操作人
     * @return 新增后的产品详情
     */
    @Transactional
    public SalesProductResponse create(SalesProductSaveRequest request, Long tenantId, String operator) {
        validateItineraryDays(request);
        assertDuplicateName(tenantId, request.productName(), null);
        SalesProductEntity entity = new SalesProductEntity();
        entity.setTenantId(tenantId);
        applyProductFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        productMapper.insert(entity);
        replaceChildren(entity.getId(), request, tenantId, operator);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改销售产品模板。
     *
     * <p>更新时先改主表，再把旧行程、旧说明和旧团队安排参数软删除并重建，确保前端四个 tab
     * 的当前内容就是产品模板的完整版本。</p>
     *
     * @param id 产品ID
     * @param request 保存请求
     * @param tenantId 当前租户ID
     * @param operator 当前操作人
     * @return 修改后的产品详情
     */
    @Transactional
    public SalesProductResponse update(Long id, SalesProductSaveRequest request, Long tenantId, String operator) {
        validateItineraryDays(request);
        assertDuplicateName(tenantId, request.productName(), id);
        SalesProductEntity entity = new SalesProductEntity();
        applyProductFields(entity, request);
        int updated = productMapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        softDeleteChildren(id, tenantId, operator);
        replaceChildren(id, request, tenantId, operator);
        return detail(id, tenantId);
    }

    /**
     * 软删除销售产品模板及其子表。
     *
     * <p>当前产品尚未接团期引用，先允许软删除。后续团期模块落地后，需要在这里增加引用保护。</p>
     */
    @Transactional
    @Override
    public void delete(Long id, Long tenantId, String operator) {
        super.delete(id, tenantId, operator);
        softDeleteChildren(id, tenantId, operator);
    }

    /** 将产品保存请求写入主表实体，并补齐默认枚举值。 */
    private void applyProductFields(SalesProductEntity entity, SalesProductSaveRequest request) {
        entity.setProductName(cleanRequired(request.productName()));
        entity.setBusinessType(clean(request.businessType()));
        entity.setDomesticInternational(SalesProductDomesticType.fromValueOrDefault(request.domesticInternational()).getValue());
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setTripType(SalesProductTripType.fromValueOrDefault(request.tripType()).getValue());
        entity.setReceptionStandard(clean(request.receptionStandard()));
        entity.setProductTheme(clean(request.productTheme()));
        entity.setTravelDays(request.travelDays() == null ? 1 : request.travelDays());
        entity.setCloseDaysBefore(number(request.closeDaysBefore()));
        entity.setSingleRoomDifference(money(request.singleRoomDifference()));
        entity.setPlannedCapacity(number(request.plannedCapacity()));
        entity.setStatus(SalesProductStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    /** 替换产品所有子表数据。 */
    private void replaceChildren(Long productId, SalesProductSaveRequest request, Long tenantId, String operator) {
        saveDescription(productId, request, tenantId, operator);
        saveItineraryDays(productId, request.itineraryDays(), tenantId, operator);
        saveArrangementItems(productId, request.arrangementItems(), tenantId, operator);
    }

    /** 保存产品说明。 */
    private void saveDescription(Long productId, SalesProductSaveRequest request, Long tenantId, String operator) {
        SalesProductDescriptionEntity entity = new SalesProductDescriptionEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setBookingNotice(clean(request.bookingNotice()));
        entity.setProductDescription(clean(request.productDescription()));
        entity.setFeeIncluded(clean(request.feeIncluded()));
        entity.setFeeExcluded(clean(request.feeExcluded()));
        entity.setChildPolicy(clean(request.childPolicy()));
        entity.setShoppingArrangement(clean(request.shoppingArrangement()));
        entity.setOptionalItems(clean(request.optionalItems()));
        entity.setGiftItems(clean(request.giftItems()));
        entity.setAttentionItems(clean(request.attentionItems()));
        entity.setWarmReminder(clean(request.warmReminder()));
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        descriptionMapper.insert(entity);
    }

    /** 保存每日行程。 */
    private void saveItineraryDays(
            Long productId,
            List<SalesProductItineraryDayRequest> itineraryDays,
            Long tenantId,
            String operator
    ) {
        if (itineraryDays == null) {
            return;
        }
        for (SalesProductItineraryDayRequest item : itineraryDays) {
            SalesProductItineraryDayEntity entity = new SalesProductItineraryDayEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setDayNo(item.dayNo());
            entity.setDayTitle(clean(item.dayTitle()));
            entity.setItineraryContent(clean(item.itineraryContent()));
            entity.setAccommodationNote(clean(item.accommodationNote()));
            entity.setRelatedHotel(clean(item.relatedHotel()));
            entity.setSeasonalSurcharge(money(item.seasonalSurcharge()));
            entity.setBreakfastIncluded(Boolean.TRUE.equals(item.breakfastIncluded()));
            entity.setLunchIncluded(Boolean.TRUE.equals(item.lunchIncluded()));
            entity.setDinnerIncluded(Boolean.TRUE.equals(item.dinnerIncluded()));
            entity.setRoadbookPlace(clean(item.roadbookPlace()));
            entity.setRoadbookSummary(clean(item.roadbookSummary()));
            entity.setRoadbookTotalDistanceMeters(number(item.roadbookTotalDistanceMeters()));
            entity.setRoadbookTotalDurationSeconds(number(item.roadbookTotalDurationSeconds()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            itineraryMapper.insert(entity);
            saveRoadbookPoints(productId, item.dayNo(), item.roadbookPoints(), tenantId, operator);
        }
    }

    /** 保存某一天的地图路书地点。 */
    private void saveRoadbookPoints(
            Long productId,
            Integer dayNo,
            List<SalesProductRoadbookPointRequest> roadbookPoints,
            Long tenantId,
            String operator
    ) {
        if (roadbookPoints == null) {
            return;
        }
        for (SalesProductRoadbookPointRequest item : roadbookPoints) {
            SalesProductRoadbookPointEntity entity = new SalesProductRoadbookPointEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setDayNo(dayNo);
            entity.setPointOrder(item.pointOrder());
            entity.setPlaceName(cleanRequired(item.placeName()));
            entity.setAddress(clean(item.address()));
            entity.setLongitude(clean(item.longitude()));
            entity.setLatitude(clean(item.latitude()));
            entity.setPointType(StringUtils.hasText(item.pointType()) ? item.pointType() : "waypoint");
            entity.setStayMinutes(number(item.stayMinutes()));
            entity.setDistanceToNextMeters(number(item.distanceToNextMeters()));
            entity.setDurationToNextSeconds(number(item.durationToNextSeconds()));
            entity.setRemark(clean(item.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            roadbookMapper.insert(entity);
        }
    }

    /** 保存团队安排参数。 */
    private void saveArrangementItems(
            Long productId,
            List<SalesProductArrangementItemRequest> arrangementItems,
            Long tenantId,
            String operator
    ) {
        if (arrangementItems == null) {
            return;
        }
        for (SalesProductArrangementItemRequest item : arrangementItems) {
            SalesProductArrangementItemEntity entity = new SalesProductArrangementItemEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setArrangementType(SalesProductArrangementType.fromValue(item.arrangementType()).getValue());
            entity.setItemName(cleanRequired(item.itemName()));
            entity.setArrangementContent(clean(item.arrangementContent()));
            entity.setQuantity(decimal(item.quantity()));
            entity.setUnitPrice(decimal(item.unitPrice()));
            entity.setUnitName(clean(item.unitName()));
            entity.setSettlementType(SalesProductSettlementType.fromValueOrDefault(item.settlementType()).getValue());
            entity.setAllocationMode(clean(item.allocationMode()));
            entity.setScheduleStartDay(clean(item.scheduleStartDay()));
            entity.setScheduleEndDay(clean(item.scheduleEndDay()));
            entity.setDeparturePlace(clean(item.departurePlace()));
            entity.setArrivalPlace(clean(item.arrivalPlace()));
            entity.setDaysCount(number(item.daysCount()));
            entity.setResourceName(clean(item.resourceName()));
            entity.setSupplierId(item.supplierId());
            entity.setSupplierName(clean(item.supplierName()));
            entity.setDriverName(clean(item.driverName()));
            entity.setVehiclePlate(clean(item.vehiclePlate()));
            entity.setTrafficType(clean(item.trafficType()));
            entity.setVehicleType(clean(item.vehicleType()));
            entity.setMealType(clean(item.mealType()));
            entity.setFundIncluded(clean(item.fundIncluded()));
            entity.setConfirmed(Boolean.TRUE.equals(item.confirmed()));
            entity.setConfirmationNo(clean(item.confirmationNo()));
            entity.setGuideId(item.guideId());
            entity.setGuideName(clean(item.guideName()));
            entity.setResponsibleEmployeeId(item.responsibleEmployeeId());
            entity.setResponsibleEmployeeName(clean(item.responsibleEmployeeName()));
            entity.setOrderScope(StringUtils.hasText(item.orderScope()) ? clean(item.orderScope()) : "=不关联订单=");
            entity.setTotalAmount(decimal(item.totalAmount()));
            entity.setCashAmount(decimal(item.cashAmount()));
            entity.setCreditAmount(decimal(item.creditAmount()));
            entity.setPrepaidAmount(decimal(item.prepaidAmount()));
            entity.setSaleAmount(decimal(item.saleAmount()));
            entity.setCostAmount(decimal(item.costAmount()));
            entity.setGuideCommissionAmount(decimal(item.guideCommissionAmount()));
            entity.setCompanyRebateAmount(decimal(item.companyRebateAmount()));
            entity.setHeadFeeAmount(decimal(item.headFeeAmount()));
            entity.setConsumptionAmount(decimal(item.consumptionAmount()));
            entity.setPeopleCount(decimal(item.peopleCount()));
            entity.setNoGuideReport(Boolean.TRUE.equals(item.noGuideReport()));
            entity.setRemark(clean(item.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            arrangementMapper.insert(entity);
            saveArrangementPriceLines(productId, entity.getId(), item.priceLines(), tenantId, operator);
            saveVehicleQuoteSnapshot(productId, entity.getId(), item.vehicleQuoteSnapshot(), tenantId, operator);
            saveVehicleInquiryRecords(productId, entity.getId(), item.vehicleInquiryRecords(), tenantId, operator);
        }
    }

    /** 保存用车报价测算快照。非用车安排也允许为空，避免通用安排保存被迫携带用车字段。 */
    private void saveVehicleQuoteSnapshot(
            Long productId,
            Long arrangementItemId,
            SalesProductVehicleQuoteSnapshotRequest item,
            Long tenantId,
            String operator
    ) {
        if (item == null) {
            return;
        }
        SalesProductVehicleQuoteSnapshotEntity entity = new SalesProductVehicleQuoteSnapshotEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setArrangementItemId(arrangementItemId);
        entity.setScheduleStartDay(clean(item.scheduleStartDay()));
        entity.setScheduleEndDay(clean(item.scheduleEndDay()));
        entity.setStartDayNo(item.startDayNo());
        entity.setEndDayNo(item.endDayNo());
        entity.setSyncedDistanceMeters(number(item.syncedDistanceMeters()));
        entity.setSyncedDurationSeconds(number(item.syncedDurationSeconds()));
        entity.setRouteSummary(clean(item.routeSummary()));
        entity.setQuoteRuleId(item.quoteRuleId());
        entity.setRuleVehicleType(clean(item.ruleVehicleType()));
        entity.setRuleProvince(clean(item.ruleProvince()));
        entity.setRuleCity(clean(item.ruleCity()));
        entity.setRuleDistrict(clean(item.ruleDistrict()));
        entity.setRuleBasePrice(decimal(item.ruleBasePrice()));
        entity.setRuleBaseKilometers(decimal(item.ruleBaseKilometers()));
        entity.setRuleExtraKilometerPrice(decimal(item.ruleExtraKilometerPrice()));
        entity.setRuleMinimumPrice(decimal(item.ruleMinimumPrice()));
        entity.setRuleFloatRate(item.ruleFloatRate() == null ? BigDecimal.ONE : item.ruleFloatRate());
        entity.setCalculatedAmount(decimal(item.calculatedAmount()));
        entity.setConfirmedAmount(decimal(item.confirmedAmount()));
        entity.setRemark(clean(item.remark()));
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        vehicleQuoteSnapshotMapper.insert(entity);
    }

    /** 保存用车询价记录。每次产品保存都会重建当前页面提交的询价列表。 */
    private void saveVehicleInquiryRecords(
            Long productId,
            Long arrangementItemId,
            List<SalesProductVehicleInquiryRequest> records,
            Long tenantId,
            String operator
    ) {
        if (records == null) {
            return;
        }
        int index = 1;
        for (SalesProductVehicleInquiryRequest item : records) {
            SalesProductVehicleInquiryEntity entity = new SalesProductVehicleInquiryEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setArrangementItemId(arrangementItemId);
            entity.setSortOrder(item.sortOrder() == null ? index : item.sortOrder());
            entity.setInquiryMethod(StringUtils.hasText(item.inquiryMethod()) ? clean(item.inquiryMethod()) : "wechat_group");
            entity.setInquiryPerson(clean(item.inquiryPerson()));
            entity.setInquiryTime(item.inquiryTime());
            entity.setGroupName(clean(item.groupName()));
            entity.setSupplierId(item.supplierId());
            entity.setSupplierName(clean(item.supplierName()));
            entity.setQuotedAmount(decimal(item.quotedAmount()));
            entity.setIncludesToll(Boolean.TRUE.equals(item.includesToll()));
            entity.setIncludesParking(Boolean.TRUE.equals(item.includesParking()));
            entity.setIncludesDriverMeal(Boolean.TRUE.equals(item.includesDriverMeal()));
            entity.setIncludesDriverLodging(Boolean.TRUE.equals(item.includesDriverLodging()));
            entity.setAvailableVehicleCount(number(item.availableVehicleCount()));
            entity.setReplyPerson(clean(item.replyPerson()));
            entity.setReplyTime(item.replyTime());
            entity.setAttachmentId(item.attachmentId());
            entity.setAttachmentUrl(clean(item.attachmentUrl()));
            entity.setSelected(Boolean.TRUE.equals(item.selected()));
            entity.setRemark(clean(item.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            vehicleInquiryMapper.insert(entity);
            index++;
        }
    }

    /** 保存团队安排下的价格明细行。 */
    private void saveArrangementPriceLines(
            Long productId,
            Long arrangementItemId,
            List<SalesProductArrangementPriceLineRequest> priceLines,
            Long tenantId,
            String operator
    ) {
        if (priceLines == null) {
            return;
        }
        int index = 1;
        for (SalesProductArrangementPriceLineRequest item : priceLines) {
            SalesProductArrangementPriceLineEntity entity = new SalesProductArrangementPriceLineEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setArrangementItemId(arrangementItemId);
            entity.setProjectId(item.projectId());
            entity.setProjectName(clean(item.projectName()));
            entity.setUnitPrice(decimal(item.unitPrice()));
            entity.setQuantity(decimal(item.quantity()));
            entity.setAmount(decimal(item.amount()));
            entity.setSalePrice(decimal(item.salePrice()));
            entity.setCostPrice(decimal(item.costPrice()));
            entity.setCashAmount(decimal(item.cashAmount()));
            entity.setCreditAmount(decimal(item.creditAmount()));
            entity.setGuideCommissionAmount(decimal(item.guideCommissionAmount()));
            entity.setGuideCommissionRate(decimal(item.guideCommissionRate()));
            entity.setCompanyRebateAmount(decimal(item.companyRebateAmount()));
            entity.setHeadFeeAmount(decimal(item.headFeeAmount()));
            entity.setConsumptionAmount(decimal(item.consumptionAmount()));
            entity.setSortOrder(item.sortOrder() == null ? index : item.sortOrder());
            entity.setRemark(clean(item.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            priceLineMapper.insert(entity);
            index++;
        }
    }

    /** 软删除产品的所有子表记录。 */
    private void softDeleteChildren(Long productId, Long tenantId, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        SalesProductItineraryDayEntity itinerary = new SalesProductItineraryDayEntity();
        itinerary.setIsDeleted(true);
        itinerary.setDeletedAt(now);
        itinerary.setDeletedBy(operator);
        itineraryMapper.update(itinerary, childUpdate(tenantId, productId));

        SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
        description.setIsDeleted(true);
        description.setDeletedAt(now);
        description.setDeletedBy(operator);
        descriptionMapper.update(description, childUpdate(tenantId, productId));

        SalesProductArrangementItemEntity arrangement = new SalesProductArrangementItemEntity();
        arrangement.setIsDeleted(true);
        arrangement.setDeletedAt(now);
        arrangement.setDeletedBy(operator);
        arrangementMapper.update(arrangement, childUpdate(tenantId, productId));

        SalesProductArrangementPriceLineEntity priceLine = new SalesProductArrangementPriceLineEntity();
        priceLine.setIsDeleted(true);
        priceLine.setDeletedAt(now);
        priceLine.setDeletedBy(operator);
        priceLineMapper.update(priceLine, childUpdate(tenantId, productId));

        SalesProductRoadbookPointEntity roadbook = new SalesProductRoadbookPointEntity();
        roadbook.setIsDeleted(true);
        roadbook.setDeletedAt(now);
        roadbook.setDeletedBy(operator);
        roadbookMapper.update(roadbook, childUpdate(tenantId, productId));

        SalesProductVehicleQuoteSnapshotEntity vehicleQuote = new SalesProductVehicleQuoteSnapshotEntity();
        vehicleQuote.setIsDeleted(true);
        vehicleQuote.setDeletedAt(now);
        vehicleQuote.setDeletedBy(operator);
        vehicleQuoteSnapshotMapper.update(vehicleQuote, childUpdate(tenantId, productId));

        SalesProductVehicleInquiryEntity vehicleInquiry = new SalesProductVehicleInquiryEntity();
        vehicleInquiry.setIsDeleted(true);
        vehicleInquiry.setDeletedAt(now);
        vehicleInquiry.setDeletedBy(operator);
        vehicleInquiryMapper.update(vehicleInquiry, childUpdate(tenantId, productId));
    }

    /** 构造子表更新条件，所有子表修改都必须带租户和产品边界。 */
    private <T> com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<T> childUpdate(Long tenantId, Long productId) {
        return new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<T>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .eq("is_deleted", false);
    }

    /** 校验同租户未删除产品名称唯一。 */
    private void assertDuplicateName(Long tenantId, String productName, Long excludeId) {
        Long count = productMapper.selectCount(baseQuery(tenantId)
                .eq("product_name", cleanRequired(productName))
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("产品名称已存在");
        }
    }

    /** 校验行程天数不能重复，也不能超过产品旅游天数。 */
    private void validateItineraryDays(SalesProductSaveRequest request) {
        if (request.itineraryDays() == null) {
            return;
        }
        Set<Integer> dayNos = new HashSet<>();
        int travelDays = request.travelDays() == null ? 1 : request.travelDays();
        for (SalesProductItineraryDayRequest item : request.itineraryDays()) {
            if (item.dayNo() == null || item.dayNo() < 1) {
                throw new BizException("行程天数必须从1开始");
            }
            if (item.dayNo() > travelDays) {
                throw new BizException("行程天数不能超过产品旅游天数");
            }
            if (!dayNos.add(item.dayNo())) {
                throw new BizException("行程天数不能重复");
            }
        }
    }

    /** 组装详情响应。 */
    private SalesProductResponse detailResponse(SalesProductEntity product) {
        Long tenantId = product.getTenantId();
        Long productId = product.getId();
        SalesProductDescriptionEntity description = descriptionMapper.selectOne(new QueryWrapper<SalesProductDescriptionEntity>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .eq("is_deleted", false));
        List<SalesProductRoadbookPointEntity> roadbookPoints = roadbookMapper.selectList(
                new QueryWrapper<SalesProductRoadbookPointEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .orderByAsc("day_no")
                        .orderByAsc("point_order")
                        .orderByAsc("id"));
        Map<Integer, List<SalesProductRoadbookPointResponse>> roadbookByDay = roadbookPoints.stream()
                .collect(Collectors.groupingBy(
                        SalesProductRoadbookPointEntity::getDayNo,
                        Collectors.mapping(SalesProductRoadbookPointResponse::fromEntity, Collectors.toList())
                ));
        List<SalesProductItineraryDayResponse> itineraryDays = itineraryMapper.selectList(
                        new QueryWrapper<SalesProductItineraryDayEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("product_id", productId)
                                .eq("is_deleted", false)
                                .orderByAsc("day_no")
                                .orderByAsc("id"))
                .stream()
                .map(item -> SalesProductItineraryDayResponse.fromEntity(
                        item,
                        roadbookByDay.getOrDefault(item.getDayNo(), List.of())
                ))
                .toList();
        List<SalesProductArrangementPriceLineEntity> priceLines = priceLineMapper.selectList(
                new QueryWrapper<SalesProductArrangementPriceLineEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .orderByAsc("arrangement_item_id")
                        .orderByAsc("sort_order")
                        .orderByAsc("id"));
        Map<Long, List<SalesProductArrangementPriceLineResponse>> priceLinesByItem = priceLines.stream()
                .collect(Collectors.groupingBy(
                        SalesProductArrangementPriceLineEntity::getArrangementItemId,
                        Collectors.mapping(SalesProductArrangementPriceLineResponse::fromEntity, Collectors.toList())
                ));
        Map<Long, SalesProductVehicleQuoteSnapshotResponse> vehicleQuoteByItem = vehicleQuoteSnapshotMapper.selectList(
                        new QueryWrapper<SalesProductVehicleQuoteSnapshotEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("product_id", productId)
                                .eq("is_deleted", false)
                                .orderByAsc("arrangement_item_id")
                                .orderByDesc("id"))
                .stream()
                .collect(Collectors.toMap(
                        SalesProductVehicleQuoteSnapshotEntity::getArrangementItemId,
                        SalesProductVehicleQuoteSnapshotResponse::fromEntity,
                        (first, ignored) -> first
                ));
        Map<Long, List<SalesProductVehicleInquiryResponse>> vehicleInquiriesByItem = vehicleInquiryMapper.selectList(
                        new QueryWrapper<SalesProductVehicleInquiryEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("product_id", productId)
                                .eq("is_deleted", false)
                                .orderByAsc("arrangement_item_id")
                                .orderByAsc("sort_order")
                                .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        SalesProductVehicleInquiryEntity::getArrangementItemId,
                        Collectors.mapping(SalesProductVehicleInquiryResponse::fromEntity, Collectors.toList())
                ));
        List<SalesProductArrangementItemResponse> arrangementItems = arrangementMapper.selectList(
                new QueryWrapper<SalesProductArrangementItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .orderByAsc("arrangement_type")
                        .orderByAsc("id"))
                .stream()
                .map(item -> SalesProductArrangementItemResponse.fromEntity(
                        item,
                        priceLinesByItem.getOrDefault(item.getId(), List.of()),
                        vehicleQuoteByItem.get(item.getId()),
                        vehicleInquiriesByItem.getOrDefault(item.getId(), List.of())
                ))
                .toList();
        return SalesProductResponse.fromDetail(product, description, itineraryDays, arrangementItems);
    }

    /** 将空金额转为 0。 */
    private BigDecimal decimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    protected SalesProductEntity newEntity() {
        return new SalesProductEntity();
    }

    @Override
    protected SalesProductResponse toResponse(SalesProductEntity entity) {
        return SalesProductResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "产品不存在或已删除";
    }
}
