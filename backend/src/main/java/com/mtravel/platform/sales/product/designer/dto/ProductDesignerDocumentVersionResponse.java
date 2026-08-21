package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;

/** 产品对外文档版本返回对象。 */
public record ProductDesignerDocumentVersionResponse(
        Long id,
        Long productId,
        String documentType,
        Integer versionNo,
        String fileName,
        String generateStatus,
        String downloadUrl,
        String previewUrl
) {
    /** 将文档版本实体转换为页面可用的下载记录。 */
    public static ProductDesignerDocumentVersionResponse fromEntity(SalesProductDocumentVersionEntity entity) {
        return new ProductDesignerDocumentVersionResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getDocumentType(),
                entity.getVersionNo(),
                entity.getFileNameSnapshot(),
                entity.getGenerateStatus(),
                "/sales/product/designer/documents/%d/download".formatted(entity.getId()),
                "/sales/product/designer/documents/%d/preview".formatted(entity.getId())
        );
    }
}
