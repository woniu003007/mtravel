package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 销售报价统一审批配置保存请求。
 */
public record QuoteApprovalConfigRequest(
        /** 审批人列表，保存顺序即审批顺序。 */
        @Valid
        List<QuoteApprovalMemberRequest> approvers,

        /** 抄送人列表，审批通过后可见。 */
        @Valid
        List<QuoteApprovalMemberRequest> ccUsers
) {}
