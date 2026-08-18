package com.mtravel.platform.common.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/**
 * 知识处理任务实体，对应 knowledge_processing_tasks 表。
 *
 * <p>任务表记录抽取、OCR 和向量化的执行状态。删除文档时未完成任务会被取消，后台执行前
 * 必须校验文档版本和删除状态。</p>
 */
@TableName("knowledge_processing_tasks")
public class KnowledgeProcessingTaskEntity extends TenantSoftDeleteEntity {

    /** 知识文档 ID。 */
    @TableField("document_id")
    private Long documentId;

    /** 任务类型：extract、index。 */
    @TableField("task_type")
    private String taskType;

    /** 任务状态：pending、running、succeeded、failed、cancelled。 */
    @TableField("task_status")
    private String taskStatus;

    /** 对应文档索引版本。 */
    @TableField("index_version")
    private Integer indexVersion;

    /** 已重试次数。 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 下次可重试时间。 */
    @TableField("next_retry_at")
    private OffsetDateTime nextRetryAt;

    /** 任务锁持有者。 */
    @TableField("locked_by")
    private String lockedBy;

    /** 任务加锁时间。 */
    @TableField("locked_at")
    private OffsetDateTime lockedAt;

    /** 最近一次失败原因。 */
    @TableField("error_message")
    private String errorMessage;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
    public Integer getIndexVersion() { return indexVersion; }
    public void setIndexVersion(Integer indexVersion) { this.indexVersion = indexVersion; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public OffsetDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(OffsetDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public OffsetDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(OffsetDateTime lockedAt) { this.lockedAt = lockedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
