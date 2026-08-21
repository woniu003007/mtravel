package com.mtravel.platform.sales.product.designer.dto;

/** 当前 Word 方案中的素材及其来源景区。 */
public record ProductDesignerDayWordPlanMaterialResponse(
        Long dayResourceId,
        Long resourceId,
        String resourceName,
        ProductDesignerSelectedMaterialResponse material
) {}
