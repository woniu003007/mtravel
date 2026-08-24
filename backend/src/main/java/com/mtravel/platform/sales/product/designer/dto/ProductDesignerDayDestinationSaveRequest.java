package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 产品设计工作台当天主行程城市保存请求。 */
public record ProductDesignerDayDestinationSaveRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "行程天数不能为空")
        @Min(value = 1, message = "行程天数必须从1开始") Integer dayNo,
        @Size(max = 80, message = "目的地省份不能超过80个字符") String destinationProvince,
        @NotBlank(message = "当天主城市不能为空")
        @Size(max = 80, message = "当天主城市不能超过80个字符") String destinationCity,
        @Size(max = 80, message = "目的地区县不能超过80个字符") String destinationDistrict
) {}
