package com.mtravel.platform.customer.risk.dto;

import jakarta.validation.constraints.Size;

/**
 * 客户风控审批处理请求。
 *
 * @param approvalRemark 审批意见。
 */
public record CustomerRiskApprovalDecisionRequest(
        @Size(max = 500, message = "审批意见不能超过500个字符")
        String approvalRemark
) {
}
