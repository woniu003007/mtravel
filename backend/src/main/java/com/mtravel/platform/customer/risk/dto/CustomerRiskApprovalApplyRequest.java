package com.mtravel.platform.customer.risk.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 客户风控审批申请请求。
 *
 * @param customerId 客户单位 ID。
 * @param teamId 团队 ID，可为空。
 * @param orderId 订单 ID，新订单申请时可为空。
 * @param requestedAmount 本次订单预计应收金额。
 * @param remark 申请备注。
 */
public record CustomerRiskApprovalApplyRequest(
        @NotNull(message = "客户单位不能为空")
        Long customerId,
        Long teamId,
        Long orderId,
        @DecimalMin(value = "0.00", message = "申请金额不能小于0")
        BigDecimal requestedAmount,
        @Size(max = 500, message = "申请备注不能超过500个字符")
        String remark
) {
}
