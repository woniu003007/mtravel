package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 报价审批人员保存请求。
 */
public record QuoteApprovalMemberRequest(
        /** 指定审批或抄送系统用户 ID。 */
        @NotNull(message = "审批人员不能为空")
        Long systemUserId
) {}
