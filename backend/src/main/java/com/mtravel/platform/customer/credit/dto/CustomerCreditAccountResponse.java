package com.mtravel.platform.customer.credit.dto;

import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 客户授信账户返回对象。 */
public record CustomerCreditAccountResponse(
        Long id, Long customerId, String customerName, BigDecimal creditLimit,
        BigDecimal occupiedAmount, BigDecimal pendingApprovalAmount, BigDecimal availableAmount,
        BigDecimal warningThreshold, String overLimitAction, String status, String remark,
        String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CustomerCreditAccountResponse fromEntity(CustomerCreditAccountEntity entity, String customerName) {
        BigDecimal available = entity.getAvailableAmount();
        if (available == null) {
            available = safe(entity.getCreditLimit()).subtract(safe(entity.getOccupiedAmount())).subtract(safe(entity.getPendingApprovalAmount()));
        }
        return new CustomerCreditAccountResponse(
                entity.getId(), entity.getCustomerId(), customerName, entity.getCreditLimit(),
                entity.getOccupiedAmount(), entity.getPendingApprovalAmount(), available,
                entity.getWarningThreshold(), entity.getOverLimitAction(), entity.getStatus(), entity.getRemark(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
    private static BigDecimal safe(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
