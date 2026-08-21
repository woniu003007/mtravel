package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 产品设计工作台当天住宿城市和三餐保存请求。 */
public record ProductDesignerDayItinerarySaveRequest(
        Long productId,
        @Min(value = 1, message = "行程天数必须从1开始")
        Integer dayNo,
        @Size(max = 200, message = "住宿城市不能超过200个字符")
        String accommodationCity,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded
) {}
