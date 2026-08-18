package com.mtravel.platform.configuration.quote.dto;

import com.mtravel.platform.configuration.quote.entity.SalesQuoteApprovalMemberEntity;

/**
 * 报价审批人员返回对象。
 */
public record QuoteApprovalMemberResponse(
        Long id,
        String memberType,
        Long systemUserId,
        String username,
        String employeeName,
        Integer stepOrder
) {

    /** 将审批配置实体和账号快照转换为返回对象。 */
    public static QuoteApprovalMemberResponse fromEntity(
            SalesQuoteApprovalMemberEntity entity,
            String username,
            String employeeName
    ) {
        return new QuoteApprovalMemberResponse(
                entity.getId(),
                entity.getMemberType(),
                entity.getSystemUserId(),
                username,
                employeeName,
                entity.getStepOrder()
        );
    }
}
