package com.mtravel.platform.sales.product.designer.dto;

/** Word 方案中一个当天景区的当前编排和可选素材。 */
public record ProductDesignerDayWordPlanResourceResponse(
        ProductDesignerDayResourceResponse dayResource,
        ProductDesignerResourceDetailResponse resourceDetail
) {}
