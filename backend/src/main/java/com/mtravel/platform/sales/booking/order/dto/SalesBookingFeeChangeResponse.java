package com.mtravel.platform.sales.booking.order.dto;

import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderChargeLineEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 收客订单费用变更返回对象。
 */
public record SalesBookingFeeChangeResponse(
        Long id,
        String changeType,
        Long feeProjectId,
        String feeProjectName,
        String feeDescription,
        BigDecimal amount,
        String status,
        String registeredBy,
        OffsetDateTime registeredAt,
        String remark
) {
    /** 将费用变更实体转换为接口返回对象。 */
    public static SalesBookingFeeChangeResponse fromEntity(SalesBookingOrderChargeLineEntity entity) {
        return new SalesBookingFeeChangeResponse(
                entity.getId(),
                entity.getChangeType(),
                entity.getFeeProjectId(),
                entity.getFeeProjectName(),
                entity.getFeeDescription(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getRegisteredBy(),
                entity.getRegisteredAt(),
                entity.getRemark()
        );
    }
}
