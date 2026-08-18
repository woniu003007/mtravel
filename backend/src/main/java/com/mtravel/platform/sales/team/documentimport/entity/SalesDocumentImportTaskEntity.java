package com.mtravel.platform.sales.team.documentimport.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/** 团队 Word 智能代录任务实体，保存异步识别草稿和应用状态。 */
@TableName("sales_document_import_tasks")
public class SalesDocumentImportTaskEntity extends TenantSoftDeleteEntity {
    @TableField("attachment_id") private Long attachmentId;
    @TableField("target_team_id") private Long targetTeamId;
    @TableField("applied_team_id") private Long appliedTeamId;
    @TableField("source_type") private String sourceType;
    @TableField("document_type") private String documentType;
    @TableField("model_name") private String modelName;
    @TableField("status") private String status;
    @TableField("progress_percent") private Integer progressPercent;
    @TableField("draft_json") private String draftJson;
    @TableField("warnings_json") private String warningsJson;
    @TableField("error_message") private String errorMessage;
    @TableField("applied_by") private String appliedBy;
    @TableField("applied_at") private OffsetDateTime appliedAt;

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long value) { attachmentId = value; }
    public Long getTargetTeamId() { return targetTeamId; }
    public void setTargetTeamId(Long value) { targetTeamId = value; }
    public Long getAppliedTeamId() { return appliedTeamId; }
    public void setAppliedTeamId(Long value) { appliedTeamId = value; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String value) { sourceType = value; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String value) { documentType = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer value) { progressPercent = value; }
    public String getDraftJson() { return draftJson; }
    public void setDraftJson(String value) { draftJson = value; }
    public String getWarningsJson() { return warningsJson; }
    public void setWarningsJson(String value) { warningsJson = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public String getAppliedBy() { return appliedBy; }
    public void setAppliedBy(String value) { appliedBy = value; }
    public OffsetDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(OffsetDateTime value) { appliedAt = value; }
}
