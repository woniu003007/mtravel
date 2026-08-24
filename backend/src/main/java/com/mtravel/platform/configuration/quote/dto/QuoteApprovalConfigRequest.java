package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 销售报价统一审批配置保存请求。
 */
public record QuoteApprovalConfigRequest(
        /** 审批模式：department_manager 部门负责人，specified_person 指定人员。 */
        String approvalMode,

        /** 审批人列表，保存顺序即审批顺序。 */
        @Valid
        List<QuoteApprovalMemberRequest> approvers,

        /** 抄送人列表，审批通过后可见。 */
        @Valid
        List<QuoteApprovalMemberRequest> ccUsers
) {

    /** 兼容旧调用方，未传模式时按指定人员模式处理。 */
    public QuoteApprovalConfigRequest(
            List<QuoteApprovalMemberRequest> approvers,
            List<QuoteApprovalMemberRequest> ccUsers
    ) {
        this("specified_person", approvers, ccUsers);
    }
}
