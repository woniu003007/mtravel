package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 销售产品模板接口返回对象。
 *
 * <p>列表查询会返回基础字段，详情查询会带出行程、说明和团队安排参数。</p>
 */
public record SalesProductResponse(
        Long id,
        String productName,
        String businessType,
        String domesticInternational,
        String province,
        String city,
        String district,
        String tripType,
        String receptionStandard,
        String productTheme,
        Integer travelDays,
        Integer closeDaysBefore,
        BigDecimal singleRoomDifference,
        Integer plannedCapacity,
        String status,
        String bookingNotice,
        String productDescription,
        String feeIncluded,
        String feeExcluded,
        String childPolicy,
        String shoppingArrangement,
        String optionalItems,
        String giftItems,
        String attentionItems,
        String warmReminder,
        List<SalesProductItineraryDayResponse> itineraryDays,
        List<SalesProductArrangementItemResponse> arrangementItems,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 构建只有主表信息的列表响应。 */
    public static SalesProductResponse fromEntity(SalesProductEntity entity) {
        return fromDetail(entity, null, List.of(), List.of());
    }

    /** 构建产品详情响应，包含子表信息。 */
    public static SalesProductResponse fromDetail(
            SalesProductEntity entity,
            SalesProductDescriptionEntity description,
            List<SalesProductItineraryDayResponse> itineraryDays,
            List<SalesProductArrangementItemResponse> arrangementItems
    ) {
        return new SalesProductResponse(
                entity.getId(),
                entity.getProductName(),
                entity.getBusinessType(),
                entity.getDomesticInternational(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getTripType(),
                entity.getReceptionStandard(),
                entity.getProductTheme(),
                entity.getTravelDays(),
                entity.getCloseDaysBefore(),
                entity.getSingleRoomDifference(),
                entity.getPlannedCapacity(),
                entity.getStatus(),
                description == null ? null : description.getBookingNotice(),
                description == null ? null : description.getProductDescription(),
                description == null ? null : description.getFeeIncluded(),
                description == null ? null : description.getFeeExcluded(),
                description == null ? null : description.getChildPolicy(),
                description == null ? null : description.getShoppingArrangement(),
                description == null ? null : description.getOptionalItems(),
                description == null ? null : description.getGiftItems(),
                description == null ? null : description.getAttentionItems(),
                description == null ? null : description.getWarmReminder(),
                itineraryDays,
                arrangementItems,
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
