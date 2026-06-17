package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 高德路线计算点位请求。
 *
 * @param longitude 经度
 * @param latitude 纬度
 */
public record AmapRoutePointRequest(
        @NotBlank(message = "经度不能为空") String longitude,
        @NotBlank(message = "纬度不能为空") String latitude
) {}
