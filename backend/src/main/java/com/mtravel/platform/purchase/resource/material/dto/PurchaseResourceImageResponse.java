package com.mtravel.platform.purchase.resource.material.dto;

import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import java.time.OffsetDateTime;
import java.util.List;

/** 资源图片素材返回对象。 */
public record PurchaseResourceImageResponse(
        Long id,
        Long resourceId,
        Long attachmentId,
        String originalFilename,
        String fileExt,
        Long fileSize,
        List<String> tags,
        boolean isCover,
        Integer sortOrder,
        String status,
        String downloadUrl,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PurchaseResourceImageResponse fromEntity(
            PurchaseResourceImageEntity entity,
            List<String> tags
    ) {
        return new PurchaseResourceImageResponse(
                entity.getId(), entity.getResourceId(), entity.getAttachmentId(), entity.getOriginalFilename(),
                entity.getFileExt(), entity.getFileSize(), tags, Boolean.TRUE.equals(entity.getIsCover()),
                entity.getSortOrder(), entity.getStatus(),
                "/purchase/resource/%d/materials/images/%d/download".formatted(entity.getResourceId(), entity.getId()),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
