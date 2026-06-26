package com.mtravel.platform.customer.risk.dto;

import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 客户风控审批申请返回对象。
 *
 * <p>包含申请时的合同和授信快照，供总经理审批页、订单页和后续查账展示。</p>
 */
public record CustomerRiskApprovalResponse(
        Long id,
        Long customerId,
        String customerName,
        Long teamId,
        Long orderId,
        String requestNo,
        BigDecimal requestedAmount,
        List<String> riskTypes,
        String riskSummary,
        LocalDate contractExpireDate,
        BigDecimal creditLimit,
        BigDecimal occupiedAmount,
        BigDecimal pendingApprovalAmount,
        BigDecimal availableAmount,
        BigDecimal overLimitAmount,
        String status,
        String applicant,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectedBy,
        OffsetDateTime rejectedAt,
        String approvalRemark,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 从数据库实体构造接口响应。 */
    public static CustomerRiskApprovalResponse fromEntity(CustomerRiskApprovalRequestEntity entity) {
        return new CustomerRiskApprovalResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getTeamId(),
                entity.getOrderId(),
                entity.getRequestNo(),
                entity.getRequestedAmount(),
                splitRiskTypes(entity.getRiskTypes()),
                entity.getRiskSummary(),
                entity.getContractExpireDate(),
                entity.getCreditLimit(),
                entity.getOccupiedAmount(),
                entity.getPendingApprovalAmount(),
                entity.getAvailableAmount(),
                entity.getOverLimitAmount(),
                entity.getStatus(),
                entity.getApplicant(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectedBy(),
                entity.getRejectedAt(),
                entity.getApprovalRemark(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static List<String> splitRiskTypes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
