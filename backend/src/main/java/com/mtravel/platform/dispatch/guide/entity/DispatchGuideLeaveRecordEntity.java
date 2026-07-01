package com.mtravel.platform.dispatch.guide.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 导游请假记录实体。
 *
 * <p>该表保存导游申请请假、计调审批和计调直接设置不可上团的记录。已通过记录会进入
 * 导游排班汇总和团队导游安排冲突判断。</p>
 */
@TableName("dispatch_guide_leave_records")
public class DispatchGuideLeaveRecordEntity extends TenantSoftDeleteEntity {

    /** 请假导游 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 导游手机号快照。 */
    @TableField("guide_mobile")
    private String guideMobile;

    /** 来源类型。guide_apply 导游申请，dispatcher_direct 计调直接设置。 */
    @TableField("source_type")
    private String sourceType;

    /** 请假开始时间。 */
    @TableField("start_at")
    private LocalDateTime startAt;

    /** 请假结束时间。 */
    @TableField("end_at")
    private LocalDateTime endAt;

    /** 请假原因。 */
    @TableField("leave_reason")
    private String leaveReason;

    /** 请假状态。 */
    @TableField("status")
    private String status;

    /** 申请人账号或名称。 */
    @TableField("applicant")
    private String applicant;

    /** 申请时间。 */
    @TableField("applied_at")
    private OffsetDateTime appliedAt;

    /** 审批通过人。 */
    @TableField("approved_by")
    private String approvedBy;

    /** 审批通过时间。 */
    @TableField("approved_at")
    private OffsetDateTime approvedAt;

    /** 驳回人。 */
    @TableField("rejected_by")
    private String rejectedBy;

    /** 驳回时间。 */
    @TableField("rejected_at")
    private OffsetDateTime rejectedAt;

    /** 审批意见。 */
    @TableField("approval_remark")
    private String approvalRemark;

    /** 撤回人。 */
    @TableField("withdrawn_by")
    private String withdrawnBy;

    /** 撤回时间。 */
    @TableField("withdrawn_at")
    private OffsetDateTime withdrawnAt;

    public Long getGuideId() {
        return guideId;
    }

    public void setGuideId(Long guideId) {
        this.guideId = guideId;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public String getGuideMobile() {
        return guideMobile;
    }

    public void setGuideMobile(String guideMobile) {
        this.guideMobile = guideMobile;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getLeaveReason() {
        return leaveReason;
    }

    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public OffsetDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(OffsetDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public OffsetDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(OffsetDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApprovalRemark(String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    public String getWithdrawnBy() {
        return withdrawnBy;
    }

    public void setWithdrawnBy(String withdrawnBy) {
        this.withdrawnBy = withdrawnBy;
    }

    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(OffsetDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }
}
