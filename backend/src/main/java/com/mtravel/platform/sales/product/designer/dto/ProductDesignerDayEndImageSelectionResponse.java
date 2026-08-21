package com.mtravel.platform.sales.product.designer.dto;

/** 已保存的当天末尾统一图片及其跨资源全局顺序。 */
public record ProductDesignerDayEndImageSelectionResponse(
        Long dayResourceId,
        Long imageId,
        Integer sortOrder
) {}
