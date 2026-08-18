package com.mtravel.platform.customer.category.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 客户等级授信审批人员请求。
 *
 * @param systemUserId 指定系统用户 ID
 */
public record CustomerCategoryApprovalMemberRequest(
        @NotNull(message = "审批人员不能为空") Long systemUserId
) {
}
