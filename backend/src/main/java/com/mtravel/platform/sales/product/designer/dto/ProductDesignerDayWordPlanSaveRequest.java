package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.List;

/** 整组保存当天景区 Word 素材方案；数组顺序即对外内容预览顺序。 */
public record ProductDesignerDayWordPlanSaveRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer dayNo,
        @NotEmpty List<@NotNull Long> dayResourceIds,
        List<@Valid ProductDesignerDayWordPlanMaterialRequest> selectedMaterials,
        String imageMode,
        Map<Long, List<Long>> selectedImageIdsByResource,
        List<@Valid ProductDesignerDayEndImageSelectionRequest> dayEndImageSelections
) {
    public ProductDesignerDayWordPlanSaveRequest(
            Long productId,
            Integer dayNo,
            List<Long> dayResourceIds,
            List<@Valid ProductDesignerDayWordPlanMaterialRequest> selectedMaterials
    ) {
        this(productId, dayNo, dayResourceIds, selectedMaterials, null, null, null);
    }
    /** 兼容原有按资源图片选择请求。 */
    public ProductDesignerDayWordPlanSaveRequest(Long productId, Integer dayNo, List<Long> dayResourceIds,
            List<@Valid ProductDesignerDayWordPlanMaterialRequest> selectedMaterials, String imageMode,
            Map<Long, List<Long>> selectedImageIdsByResource) {
        this(productId, dayNo, dayResourceIds, selectedMaterials, imageMode, selectedImageIdsByResource, null);
    }
}
