package com.mtravel.platform.dispatch.guide.dto;

import com.mtravel.platform.dispatch.guide.entity.DispatchGuideLeaveRecordEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 导游请假记录响应。
 *
 * @param id 请假记录 ID
 * @param guideId 导游 ID
 * @param guideName 导游姓名
 * @param guideMobile 导游手机
 * @param sourceType 来源类型
 * @param startAt 开始时间
 * @param endAt 结束时间
 * @param leaveReason 请假原因
 * @param status 状态
 * @param applicant 申请人
 * @param appliedAt 申请时间
 * @param approvedBy 审批通过人
 * @param approvedAt 审批通过时间
 * @param rejectedBy 驳回人
 * @param rejectedAt 驳回时间
 * @param approvalRemark 审批意见
 * @param remark 备注
 */
public record GuideLeaveResponse(
        Long id,
        Long guideId,
        String guideName,
        String guideMobile,
        String sourceType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String leaveReason,
        String status,
        String applicant,
        OffsetDateTime appliedAt,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectedBy,
        OffsetDateTime rejectedAt,
        String approvalRemark,
        String remark
) {
    /** 将实体转换为接口响应。 */
    public static GuideLeaveResponse fromEntity(DispatchGuideLeaveRecordEntity entity) {
        return new GuideLeaveResponse(
                entity.getId(),
                entity.getGuideId(),
                entity.getGuideName(),
                entity.getGuideMobile(),
                entity.getSourceType(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getLeaveReason(),
                entity.getStatus(),
                entity.getApplicant(),
                entity.getAppliedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectedBy(),
                entity.getRejectedAt(),
                entity.getApprovalRemark(),
                entity.getRemark()
        );
    }
}
