package com.mtravel.platform.configuration.quote.dto;

import java.util.List;

/**
 * 销售报价统一审批配置返回对象。
 */
public record QuoteApprovalConfigResponse(
        String approvalMode,
        List<QuoteApprovalMemberResponse> approvers,
        List<QuoteApprovalMemberResponse> ccUsers
) {}
