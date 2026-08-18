package com.mtravel.platform.customer.risk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 客户风控校验返回对象。
 *
 * @param customerId 客户单位 ID。
 * @param customerName 客户单位名称。
 * @param teamId 团队 ID。
 * @param orderId 订单 ID，新订单可为空。
 * @param requestedAmount 本次订单预计应收金额。
 * @param riskTypes 风险类型列表。
 * @param riskSummary 风险摘要。
 * @param contractExpired 合同是否已到期。
 * @param contractExpireDate 合同有效期止。
 * @param creditOverLimit 授信是否超限。
 * @param creditLimit 授信额度。
 * @param occupiedAmount 已占用额度。
 * @param pendingApprovalAmount 审批中额度。
 * @param availableAmount 可用额度。
 * @param overLimitAmount 超限金额。
 * @param approvalEnabled 是否启用强制客户等级审批。
 * @param blocked 当前风险是否阻断订单保存。
 * @param riskApprovalRequestId 当前风险金额可复用的已通过审批申请 ID。
 * @param riskApprovalRequestNo 当前风险金额可复用的已通过审批申请编号。
 * @param riskApprovalStatus 当前风险金额可复用审批申请的状态。
 * @param riskApprovalRequestedAmount 当前风险金额可复用审批申请的审批金额。
 */
public record CustomerRiskCheckResponse(
        Long customerId,
        String customerName,
        Long teamId,
        Long orderId,
        BigDecimal requestedAmount,
        List<String> riskTypes,
        String riskSummary,
        boolean contractExpired,
        LocalDate contractExpireDate,
        boolean creditOverLimit,
        BigDecimal creditLimit,
        BigDecimal occupiedAmount,
        BigDecimal pendingApprovalAmount,
        BigDecimal availableAmount,
        BigDecimal overLimitAmount,
        boolean approvalEnabled,
        boolean blocked,
        Long riskApprovalRequestId,
        String riskApprovalRequestNo,
        String riskApprovalStatus,
        BigDecimal riskApprovalRequestedAmount
) {
}
