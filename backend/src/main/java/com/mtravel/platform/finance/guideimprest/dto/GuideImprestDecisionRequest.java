package com.mtravel.platform.finance.guideimprest.dto;

import jakarta.validation.constraints.Size;

/**
 * 导游备用金审批意见请求。
 *
 * @param approvalRemark 审批意见
 */
public record GuideImprestDecisionRequest(
        @Size(max = 500) String approvalRemark
) {
}
