package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.dto.ResourceIntroductionExtensionBlock;
import java.util.List;

/** 产品设计可选择的已发布资源介绍。 */
public record ProductDesignerIntroductionResponse(
        Long id,
        Boolean isOptionalItem,
        Long resourceOptionalItemId,
        String optionalItemName,
        String optionalItemType,
        String title,
        String tags,
        String content,
        String noticeContent,
        String warmTipContent,
        List<ResourceIntroductionExtensionBlock> extensionBlocks,
        String visitDuration,
        Integer indexVersion,
        List<Long> imageIds
) {
    public static ProductDesignerIntroductionResponse fromEntity(PurchaseResourceIntroductionEntity entity) {
        return fromEntity(entity, null, null, List.of(), List.of());
    }

    public static ProductDesignerIntroductionResponse fromEntity(
            PurchaseResourceIntroductionEntity entity,
            List<Long> imageIds
    ) {
        return fromEntity(entity, null, null, imageIds, List.of());
    }

    /** 保留原有自费项目名称映射入口，图片为空表示旧调用方未携带素材图片。 */
    public static ProductDesignerIntroductionResponse fromEntity(
            PurchaseResourceIntroductionEntity entity,
            String optionalItemName,
            String optionalItemType
    ) {
        return fromEntity(entity, optionalItemName, optionalItemType, List.of(), List.of());
    }

    public static ProductDesignerIntroductionResponse fromEntity(
            PurchaseResourceIntroductionEntity entity, List<Long> imageIds,
            List<ResourceIntroductionExtensionBlock> extensionBlocks
    ) { return fromEntity(entity, null, null, imageIds, extensionBlocks); }

    private static ProductDesignerIntroductionResponse fromEntity(
            PurchaseResourceIntroductionEntity entity,
            String optionalItemName,
            String optionalItemType,
            List<Long> imageIds,
            List<ResourceIntroductionExtensionBlock> extensionBlocks
    ) {
        return new ProductDesignerIntroductionResponse(
                entity.getId(),
                Boolean.TRUE.equals(entity.getIsOptionalItem()),
                entity.getResourceOptionalItemId(), optionalItemName, optionalItemType,
                entity.getTitle(),
                entity.getTags(),
                entity.getContent(),
                entity.getNoticeContent(),
                entity.getWarmTipContent(),
                extensionBlocks == null ? List.of() : extensionBlocks,
                entity.getVisitDuration(),
                entity.getIndexVersion(),
                imageIds == null ? List.of() : imageIds
        );
    }
}
