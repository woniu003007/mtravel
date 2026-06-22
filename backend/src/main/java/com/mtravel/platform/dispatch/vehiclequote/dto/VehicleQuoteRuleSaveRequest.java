package com.mtravel.platform.dispatch.vehiclequote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 座位数报价规则保存请求。
 *
 * @param vehicleType 座位数，例如 7座、39座。字段名沿用 vehicleType 兼容现有接口。
 * @param province 预留省份字段，当前报价规则暂不按地区区分，保存时统一写空
 * @param city 预留城市字段，当前报价规则暂不按地区区分，保存时统一写空
 * @param district 预留区县字段，当前报价规则暂不按地区区分，保存时统一写空
 * @param basePrice 基础价
 * @param baseKilometers 基础公里数
 * @param extraKilometerPrice 超公里单价
 * @param minimumPrice 最低价
 * @param floatRate 浮动系数
 * @param status 启停状态
 * @param remark 备注
 */
public record VehicleQuoteRuleSaveRequest(
        @NotBlank(message = "座位数不能为空") @Size(max = 40) String vehicleType,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @DecimalMin(value = "0", message = "基础价不能小于0") BigDecimal basePrice,
        @DecimalMin(value = "0", message = "基础公里不能小于0") BigDecimal baseKilometers,
        @DecimalMin(value = "0", message = "超公里单价不能小于0") BigDecimal extraKilometerPrice,
        @DecimalMin(value = "0", message = "最低价不能小于0") BigDecimal minimumPrice,
        @DecimalMin(value = "0", inclusive = false, message = "浮动系数必须大于0") BigDecimal floatRate,
        @Pattern(regexp = "active|disabled", message = "报价规则状态不合法") String status,
        String remark
) {}
