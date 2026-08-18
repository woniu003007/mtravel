package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;

/** 产品设计可选择的已发布资源介绍。 */
public record ProductDesignerIntroductionResponse(
        Long id,
        String title,
        String tags,
        String content,
        String noticeContent,
        Integer indexVersion
) {
    public static ProductDesignerIntroductionResponse fromEntity(PurchaseResourceIntroductionEntity entity) {
        return new ProductDesignerIntroductionResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getTags(),
                entity.getContent(),
                entity.getNoticeContent(),
                entity.getIndexVersion()
        );
    }
}
