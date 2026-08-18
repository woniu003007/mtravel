package com.mtravel.platform.common.knowledge.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/**
 * 知识文档实体，对应 knowledge_documents 表。
 *
 * <p>本表保存业务资料文件的处理状态、审核状态、抽取文本和向量索引状态。原始文件仍由
 * common_attachments 管理，向量内容拆分到 knowledge_document_chunks。</p>
 */
@TableName("knowledge_documents")
public class KnowledgeDocumentEntity extends TenantSoftDeleteEntity {

    /** 业务来源类型，例如 purchase_resource。 */
    @TableField("source_type")
    private String sourceType;

    /** 业务来源记录 ID。 */
    @TableField("source_id")
    private Long sourceId;

    /** 公共附件 ID。 */
    @TableField("attachment_id")
    private Long attachmentId;

    /** 原始上传文件名快照。 */
    @TableField("original_filename")
    private String originalFilename;

    /** 文件扩展名快照。 */
    @TableField("file_ext")
    private String fileExt;

    /** 文件大小，单位字节。 */
    @TableField("file_size")
    private Long fileSize;

    /** 文件内容 SHA-256 摘要。 */
    @TableField("file_sha256")
    private String fileSha256;

    /** 处理状态：pending、processing、succeeded、failed、deleted。 */
    @TableField("processing_status")
    private String processingStatus;

    /** 审核状态：draft、published、disabled。 */
    @TableField("review_status")
    private String reviewStatus;

    /** 向量索引状态：pending、indexed、failed、deleted。 */
    @TableField("index_status")
    private String indexStatus;

    /** 文件抽取或 OCR 后的正文文本。 */
    @TableField(value = "extracted_text", updateStrategy = FieldStrategy.ALWAYS)
    private String extractedText;

    /** 索引版本号，用于阻止旧任务复活已删除向量。 */
    @TableField("index_version")
    private Integer indexVersion;

    /** 是否作为产品手册生成资料来源。 */
    @TableField("usage_product_manual")
    private Boolean usageProductManual;

    /** 是否作为知识库问答检索来源。 */
    @TableField("usage_qa")
    private Boolean usageQa;

    /** 最近一次处理失败原因。 */
    @TableField(value = "error_message", updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;

    /** 最近一次处理完成时间。 */
    @TableField(value = "processed_at", updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime processedAt;

    /** 发布时间。 */
    @TableField(value = "published_at", updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime publishedAt;

    /** 文档状态：active、disabled。 */
    @TableField("status")
    private String status;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public Integer getIndexVersion() { return indexVersion; }
    public void setIndexVersion(Integer indexVersion) { this.indexVersion = indexVersion; }
    public Boolean getUsageProductManual() { return usageProductManual; }
    public void setUsageProductManual(Boolean usageProductManual) { this.usageProductManual = usageProductManual; }
    public Boolean getUsageQa() { return usageQa; }
    public void setUsageQa(Boolean usageQa) { this.usageQa = usageQa; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
