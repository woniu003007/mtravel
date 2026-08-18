package com.mtravel.platform.sales.booking.order.dto;

import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import java.math.BigDecimal;

/**
 * 收客订单保存结果。
 *
 * <p>保存接口只返回前端跳转和提示所需的主表摘要，避免保存成功后为了返回完整详情再次查询价格明细、
 * 游客名单和费用变更。需要完整详情时由前端显式调用详情接口。</p>
 */
public record SalesBookingOrderSaveResponse(
        Long id,
        Long teamId,
        String orderNo,
        String status,
        Integer guestCount,
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount
) {
    /** 从本次保存后的订单主实体构造轻量保存结果。 */
    public static SalesBookingOrderSaveResponse fromEntity(SalesBookingOrderEntity entity) {
        return new SalesBookingOrderSaveResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getOrderNo(),
                entity.getStatus(),
                entity.getGuestCount(),
                entity.getReceivableAmount(),
                entity.getReceivedAmount(),
                entity.getBalanceAmount()
        );
    }
}
