package com.mtravel.platform.customer.category.dto;

/**
 * 客户等级授信审批人员返回对象。
 *
 * @param systemUserId 系统用户 ID
 * @param employeeName 员工姓名
 * @param username 登录账号
 * @param stepOrder 审批顺序，抄送人返回0
 */
public record CustomerCategoryApprovalMemberResponse(
        Long systemUserId,
        String employeeName,
        String username,
        Integer stepOrder
) {
}
