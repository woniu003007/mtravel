package com.mtravel.platform.dispatch.teamarrangement.dto;

import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementOrderAllocationEntity;
import java.math.BigDecimal;

/**
 * 正式团队安排订单归属响应。
 */
public record TeamArrangementOrderAllocationResponse(
        Long id,
        String allocationScope,
        Long orderId,
        String orderNo,
        Long customerId,
        String customerName,
        Integer guestCount,
        String allocationMode,
        String splitMode,
        String splitBatchNo,
        BigDecimal originalAmount,
        BigDecimal allocationAmount,
        Integer sortOrder
) {
    /** 转换订单归属实体为响应对象。 */
    public static TeamArrangementOrderAllocationResponse fromEntity(DispatchTeamArrangementOrderAllocationEntity entity) {
        return new TeamArrangementOrderAllocationResponse(
                entity.getId(),
                entity.getAllocationScope(),
                entity.getOrderId(),
                entity.getOrderNo(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getGuestCount(),
                entity.getAllocationMode(),
                entity.getSplitMode(),
                entity.getSplitBatchNo(),
                entity.getOriginalAmount(),
                entity.getAllocationAmount(),
                entity.getSortOrder()
        );
    }
}
