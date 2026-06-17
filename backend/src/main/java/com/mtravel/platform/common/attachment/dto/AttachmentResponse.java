package com.mtravel.platform.common.attachment.dto;

import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import java.time.OffsetDateTime;

/**
 * 附件上传返回对象。
 *
 * <p>前端保存业务表单时通常只需要 attachmentId 和 fileUrl，其余字段用于页面展示和审计。</p>
 */
public record AttachmentResponse(
        Long id,
        String businessModule,
        String businessType,
        Long businessId,
        String originalFilename,
        String fileUrl,
        String contentType,
        Long fileSize,
        String fileExt,
        String status,
        String uploadedBy,
        OffsetDateTime createdAt
) {
    public static AttachmentResponse fromEntity(CommonAttachmentEntity entity) {
        return new AttachmentResponse(
                entity.getId(),
                entity.getBusinessModule(),
                entity.getBusinessType(),
                entity.getBusinessId(),
                entity.getOriginalFilename(),
                entity.getFileUrl(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getFileExt(),
                entity.getStatus(),
                entity.getUploadedBy(),
                entity.getCreatedAt()
        );
    }
}
