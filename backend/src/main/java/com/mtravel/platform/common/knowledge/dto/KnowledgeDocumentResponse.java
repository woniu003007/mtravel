package com.mtravel.platform.common.knowledge.dto;

import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentEntity;
import java.time.OffsetDateTime;

/**
 * 知识文档返回对象。
 *
 * @param id 知识文档 ID
 * @param attachmentId 原始附件 ID
 * @param originalFilename 原始文件名
 * @param processingStatus 处理状态
 * @param reviewStatus 审核状态
 * @param indexStatus 向量索引状态
 */
public record KnowledgeDocumentResponse(
        Long id,
        String sourceType,
        Long sourceId,
        Long attachmentId,
        String downloadUrl,
        String originalFilename,
        String fileExt,
        Long fileSize,
        String fileSha256,
        String processingStatus,
        String reviewStatus,
        String indexStatus,
        Integer indexVersion,
        Boolean usageProductManual,
        Boolean usageQa,
        String errorMessage,
        OffsetDateTime processedAt,
        OffsetDateTime publishedAt,
        String status,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将知识文档实体转换为接口返回结构。 */
    public static KnowledgeDocumentResponse fromEntity(KnowledgeDocumentEntity entity) {
        return new KnowledgeDocumentResponse(
                entity.getId(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getAttachmentId(),
                null,
                entity.getOriginalFilename(),
                entity.getFileExt(),
                entity.getFileSize(),
                entity.getFileSha256(),
                entity.getProcessingStatus(),
                entity.getReviewStatus(),
                entity.getIndexStatus(),
                entity.getIndexVersion(),
                entity.getUsageProductManual(),
                entity.getUsageQa(),
                entity.getErrorMessage(),
                entity.getProcessedAt(),
                entity.getPublishedAt(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
