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
 * <p>包含申请时的合同和授信快照，供客户等级审批页、订单页和后续查账展示。</p>
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
        Long categoryId,
        String categoryName,
        Integer creditTermDays,
        Integer currentApprovalStep,
        String status,
        Long applicantUserId,
        String applicant,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectedBy,
        OffsetDateTime rejectedAt,
        String approvalRemark,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<CustomerRiskApprovalStepResponse> approvalSteps,
        List<CustomerRiskApprovalCcResponse> ccUsers,
        boolean canApprove
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
                entity.getCategoryId(),
                entity.getCategoryName(),
                entity.getCreditTermDays(),
                entity.getCurrentApprovalStep(),
                entity.getStatus(),
                entity.getApplicantUserId(),
                entity.getApplicant(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectedBy(),
                entity.getRejectedAt(),
                entity.getApprovalRemark(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                List.of(),
                List.of(),
                false
        );
    }

    /**
     * 从申请主单和人员快照构造审批页响应。
     */
    public static CustomerRiskApprovalResponse fromEntity(
            CustomerRiskApprovalRequestEntity entity,
            List<CustomerRiskApprovalStepResponse> approvalSteps,
            List<CustomerRiskApprovalCcResponse> ccUsers,
            boolean canApprove
    ) {
        CustomerRiskApprovalResponse base = fromEntity(entity);
        return new CustomerRiskApprovalResponse(
                base.id(), base.customerId(), base.customerName(), base.teamId(), base.orderId(), base.requestNo(),
                base.requestedAmount(), base.riskTypes(), base.riskSummary(), base.contractExpireDate(),
                base.creditLimit(), base.occupiedAmount(), base.pendingApprovalAmount(), base.availableAmount(),
                base.overLimitAmount(), base.categoryId(), base.categoryName(), base.creditTermDays(),
                base.currentApprovalStep(), base.status(), base.applicantUserId(), base.applicant(), base.approvedBy(),
                base.approvedAt(), base.rejectedBy(), base.rejectedAt(), base.approvalRemark(), base.remark(),
                base.createdBy(), base.createdAt(), base.updatedAt(),
                approvalSteps == null ? List.of() : approvalSteps,
                ccUsers == null ? List.of() : ccUsers,
                canApprove
        );
    }

    /** 兼容订单服务旧测试数据的构造方式。 */
    public CustomerRiskApprovalResponse(
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
        this(
                id, customerId, customerName, teamId, orderId, requestNo, requestedAmount, riskTypes, riskSummary,
                contractExpireDate, creditLimit, occupiedAmount, pendingApprovalAmount, availableAmount, overLimitAmount,
                null, null, null, null, status, null, applicant, approvedBy, approvedAt, rejectedBy, rejectedAt,
                approvalRemark, remark, createdBy, createdAt, updatedAt, List.of(), List.of(), false
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
