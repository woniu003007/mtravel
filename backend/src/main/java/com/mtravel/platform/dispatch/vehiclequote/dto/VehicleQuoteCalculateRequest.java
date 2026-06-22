package com.mtravel.platform.dispatch.vehiclequote.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用车报价测算请求。
 *
 * @param vehicleType 座位数，例如 7座、39座。字段名沿用 vehicleType 兼容现有接口。
 * @param province 预留省份字段，当前报价规则暂不按地区区分
 * @param city 预留城市字段，当前报价规则暂不按地区区分
 * @param district 预留区县字段，当前报价规则暂不按地区区分
 * @param distanceMeters 路书距离，单位米
 */
public record VehicleQuoteCalculateRequest(
        @NotBlank(message = "座位数不能为空") @Size(max = 40) String vehicleType,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @Min(value = 0, message = "路书距离不能小于0") Integer distanceMeters
) {}
