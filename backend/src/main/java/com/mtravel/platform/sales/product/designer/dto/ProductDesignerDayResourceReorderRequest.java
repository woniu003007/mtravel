package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 产品设计工作台当天资源排序请求，resourceIds 按前端显示顺序传入。 */
public record ProductDesignerDayResourceReorderRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "行程天数不能为空") @Min(value = 1, message = "行程天数必须从1开始") Integer dayNo,
        @NotEmpty(message = "排序资源不能为空") List<Long> dayResourceIds
) {}
