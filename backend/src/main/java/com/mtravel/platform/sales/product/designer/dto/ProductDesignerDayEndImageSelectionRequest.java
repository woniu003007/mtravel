package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.NotNull;

/** 当天末尾统一图片中的一项；数组顺序即 Word 图片顺序。 */
public record ProductDesignerDayEndImageSelectionRequest(
        @NotNull Long dayResourceId,
        @NotNull Long imageId
) {}
