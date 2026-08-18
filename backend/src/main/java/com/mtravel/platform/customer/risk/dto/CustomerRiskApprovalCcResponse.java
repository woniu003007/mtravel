package com.mtravel.platform.customer.risk.dto;

import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalCcEntity;
import java.time.OffsetDateTime;

/**
 * 客户授信超额审批抄送返回对象。
 *
 * @param ccUserId 抄送人系统用户 ID
 * @param ccName 抄送人姓名快照
 * @param visibleAt 抄送可见时间，全部审批通过前为空
 */
public record CustomerRiskApprovalCcResponse(
        Long ccUserId,
        String ccName,
        OffsetDateTime visibleAt
) {
    /** 从抄送快照构造接口响应。 */
    public static CustomerRiskApprovalCcResponse fromEntity(CustomerRiskApprovalCcEntity entity) {
        return new CustomerRiskApprovalCcResponse(entity.getCcUserId(), entity.getCcName(), entity.getVisibleAt());
    }
}
