package com.mtravel.platform.purchase.resource.material.dto;

import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import java.time.OffsetDateTime;
import java.util.List;

/** 资源介绍素材返回对象。 */
public record PurchaseResourceIntroductionResponse(
        Long id,
        Long resourceId,
        Integer sortOrder,
        Boolean isOptionalItem,
        Long resourceOptionalItemId,
        String resourceOptionalItemName,
        String title,
        List<String> tags,
        String content,
        String noticeContent,
        String warmTipContent,
        List<ResourceIntroductionExtensionBlock> extensionBlocks,
        String visitDuration,
        String status,
        String indexStatus,
        Integer indexVersion,
        String errorMessage,
        OffsetDateTime publishedAt,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PurchaseResourceIntroductionResponse fromEntity(
            PurchaseResourceIntroductionEntity entity,
            List<String> tags,
            List<ResourceIntroductionExtensionBlock> extensionBlocks
    ) {
        return fromEntity(entity, tags, extensionBlocks, null);
    }
    public static PurchaseResourceIntroductionResponse fromEntity(PurchaseResourceIntroductionEntity entity, List<String> tags, List<ResourceIntroductionExtensionBlock> extensionBlocks, String resourceOptionalItemName) {
        return new PurchaseResourceIntroductionResponse(
                entity.getId(), entity.getResourceId(), entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getIsOptionalItem()), entity.getResourceOptionalItemId(), resourceOptionalItemName, entity.getTitle(), tags, entity.getContent(), entity.getNoticeContent(),
                entity.getWarmTipContent(), extensionBlocks == null ? List.of() : extensionBlocks, entity.getVisitDuration(),
                entity.getStatus(), entity.getIndexStatus(), entity.getIndexVersion(), entity.getErrorMessage(),
                entity.getPublishedAt(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
