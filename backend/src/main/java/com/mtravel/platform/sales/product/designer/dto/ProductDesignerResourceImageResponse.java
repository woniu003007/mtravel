package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;

/** 产品设计可选择的资源图片素材摘要。 */
public record ProductDesignerResourceImageResponse(
        Long id,
        Long attachmentId,
        String originalFilename,
        String fileExt,
        Boolean isCover,
        Integer sortOrder
) {
    public static ProductDesignerResourceImageResponse fromEntity(PurchaseResourceImageEntity entity) {
        return new ProductDesignerResourceImageResponse(
                entity.getId(),
                entity.getAttachmentId(),
                entity.getOriginalFilename(),
                entity.getFileExt(),
                entity.getIsCover(),
                entity.getSortOrder()
        );
    }
}
