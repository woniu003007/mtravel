package com.mtravel.platform.sales.team.documentimport.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/** 团队文档代录正式写入幂等记录实体。 */
@TableName("sales_document_import_apply_records")
public class SalesDocumentImportApplyRecordEntity extends TenantSoftDeleteEntity {
    @TableField("task_id") private Long taskId;
    @TableField("target_type") private String targetType;
    @TableField("target_id") private Long targetId;
    @TableField("draft_item_key") private String draftItemKey;
    @TableField("status") private String status;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long value) { taskId = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { targetType = value; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long value) { targetId = value; }
    public String getDraftItemKey() { return draftItemKey; }
    public void setDraftItemKey(String value) { draftItemKey = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
}
