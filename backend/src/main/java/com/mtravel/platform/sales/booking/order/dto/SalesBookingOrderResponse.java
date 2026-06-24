package com.mtravel.platform.sales.booking.order.dto;

import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 收客订单详情返回对象。
 *
 * <p>返回旧系统收客页需要的订单主信息、价格明细、游客名单和费用变更记录，不暴露租户和软删除字段。</p>
 */
public record SalesBookingOrderResponse(
        Long id,
        Long teamId,
        String orderNo,
        Long customerId,
        String customerName,
        String contactName,
        String contactPhone,
        String customerTeamNo,
        String sourceProvince,
        String sourceCity,
        String sourceDistrict,
        String travelDescription,
        String pickupInfo,
        String dropoffInfo,
        String pickupRemark,
        String guideName,
        String guidePhone,
        String guideRemark,
        String hotelInfo,
        Integer adultCount,
        Integer childCount,
        Integer childNoBedCount,
        Integer seniorCount,
        Integer escortCount,
        Integer guestCount,
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount,
        String feeRemark,
        String confirmRemark,
        String orderRemark,
        String status,
        String bookedBy,
        OffsetDateTime bookedAt,
        String remark,
        List<SalesBookingOrderPriceLineResponse> priceLines,
        List<SalesBookingOrderGuestResponse> guests,
        List<SalesBookingFeeChangeResponse> feeChanges
) {
    /** 将订单主实体和子表转换为详情返回对象。 */
    public static SalesBookingOrderResponse fromEntity(
            SalesBookingOrderEntity entity,
            List<SalesBookingOrderPriceLineResponse> priceLines,
            List<SalesBookingOrderGuestResponse> guests,
            List<SalesBookingFeeChangeResponse> feeChanges
    ) {
        return new SalesBookingOrderResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getOrderNo(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getCustomerTeamNo(),
                entity.getSourceProvince(),
                entity.getSourceCity(),
                entity.getSourceDistrict(),
                entity.getTravelDescription(),
                entity.getPickupInfo(),
                entity.getDropoffInfo(),
                entity.getPickupRemark(),
                entity.getGuideName(),
                entity.getGuidePhone(),
                entity.getGuideRemark(),
                entity.getHotelInfo(),
                entity.getAdultCount(),
                entity.getChildCount(),
                entity.getChildNoBedCount(),
                entity.getSeniorCount(),
                entity.getEscortCount(),
                entity.getGuestCount(),
                entity.getReceivableAmount(),
                entity.getReceivedAmount(),
                entity.getBalanceAmount(),
                entity.getFeeRemark(),
                entity.getConfirmRemark(),
                entity.getOrderRemark(),
                entity.getStatus(),
                entity.getBookedBy(),
                entity.getBookedAt(),
                entity.getRemark(),
                priceLines == null ? List.of() : priceLines,
                guests == null ? List.of() : guests,
                feeChanges == null ? List.of() : feeChanges
        );
    }
}
