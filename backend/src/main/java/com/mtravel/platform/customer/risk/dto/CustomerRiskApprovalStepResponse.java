package com.mtravel.platform.customer.risk.dto;

import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalStepEntity;
import java.time.OffsetDateTime;

/**
 * 客户授信超额审批步骤返回对象。
 *
 * @param stepOrder 审批顺序
 * @param approverUserId 审批人系统用户 ID
 * @param approverName 审批人姓名快照
 * @param status 步骤状态
 * @param decidedAt 处理时间
 * @param decisionRemark 本步审批意见
 */
public record CustomerRiskApprovalStepResponse(
        Integer stepOrder,
        Long approverUserId,
        String approverName,
        String status,
        OffsetDateTime decidedAt,
        String decisionRemark
) {
    /** 从审批步骤快照构造接口响应。 */
    public static CustomerRiskApprovalStepResponse fromEntity(CustomerRiskApprovalStepEntity entity) {
        return new CustomerRiskApprovalStepResponse(
                entity.getStepOrder(),
                entity.getApproverUserId(),
                entity.getApproverName(),
                entity.getStatus(),
                entity.getDecidedAt(),
                entity.getDecisionRemark()
        );
    }
}
