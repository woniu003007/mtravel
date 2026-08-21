package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.dto.ResourceIntroductionExtensionBlock;
import java.util.List;

/** 产品每日资源已选介绍素材的快照响应，按 sortOrder 输出。 */
public record ProductDesignerIntroductionSnapshotResponse(
        Long id,
        Long resourceIntroductionId,
        Integer introductionIndexVersion,
        String title,
        String content,
        String noticeContent,
        String warmTipContent,
        List<ResourceIntroductionExtensionBlock> extensionBlocks,
        String visitDuration,
        Integer sortOrder
) {
    public static ProductDesignerIntroductionSnapshotResponse fromEntity(
            SalesProductDayResourceIntroductionEntity entity
    ) {
        return fromEntity(entity, List.of());
    }

    public static ProductDesignerIntroductionSnapshotResponse fromEntity(
            SalesProductDayResourceIntroductionEntity entity,
            List<ResourceIntroductionExtensionBlock> extensionBlocks
    ) {
        return new ProductDesignerIntroductionSnapshotResponse(
                entity.getId(),
                entity.getResourceIntroductionId(),
                entity.getIntroductionIndexVersion(),
                entity.getTitleSnapshot(),
                entity.getContentSnapshot(),
                entity.getNoticeSnapshot(),
                entity.getWarmTipSnapshot(),
                extensionBlocks == null ? List.of() : extensionBlocks,
                entity.getVisitDurationSnapshot(),
                entity.getSortOrder()
        );
    }
}
