package com.mtravel.platform.dispatch.guide.dto;

import jakarta.validation.constraints.Size;

/**
 * 导游请假审批请求。
 *
 * @param approvalRemark 审批意见
 */
public record GuideLeaveReviewRequest(
        @Size(max = 1000, message = "审批意见不能超过1000字") String approvalRemark
) {
}
