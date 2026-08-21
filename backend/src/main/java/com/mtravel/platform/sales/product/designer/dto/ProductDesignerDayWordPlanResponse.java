package com.mtravel.platform.sales.product.designer.dto;

import java.util.List;

/** 当天景区组合的 Word 方案详情。 */
public record ProductDesignerDayWordPlanResponse(
        Long productId,
        Integer dayNo,
        List<ProductDesignerDayWordPlanResourceResponse> resources,
        List<ProductDesignerDayWordPlanMaterialResponse> selectedMaterials,
        String imageMode,
        List<ProductDesignerDayEndImageSelectionResponse> dayEndImageSelections
) {
    /** 兼容旧调用方：未配置时按景区跟随方式处理。 */
    public ProductDesignerDayWordPlanResponse(
            Long productId,
            Integer dayNo,
            List<ProductDesignerDayWordPlanResourceResponse> resources,
            List<ProductDesignerDayWordPlanMaterialResponse> selectedMaterials
    ) {
        this(productId, dayNo, resources, selectedMaterials, "follow_resource", List.of());
    }
    public ProductDesignerDayWordPlanResponse(Long productId, Integer dayNo,
            List<ProductDesignerDayWordPlanResourceResponse> resources,
            List<ProductDesignerDayWordPlanMaterialResponse> selectedMaterials, String imageMode) {
        this(productId, dayNo, resources, selectedMaterials, imageMode, List.of());
    }
}
