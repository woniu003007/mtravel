package com.mtravel.platform.purchase.resourcequote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 普通资源报价规则保存请求。
 *
 * @param resourceType 资源类型
 * @param customerLevelId 可选客户等级 ID；为空时保存默认规则
 * @param suggestedRate 建议比例上浮，按百分数保存
 * @param minimumRate 最低比例上浮，按百分数保存
 * @param suggestedFixedAddon 建议固定加价，单位为元
 * @param minimumFixedAddon 最低固定加价，单位为元
 * @param status 规则状态，active 或 disabled
 * @param remark 规则备注
 */
public record ResourceQuoteRuleSaveRequest(
        @NotBlank(message = "资源类型不能为空")
        @Size(max = 40, message = "资源类型长度不能超过40")
        @Pattern(
                regexp = "hotel|scenic|vehicle|restaurant|guide|ground_agent|ticket|shopping|other",
                message = "资源类型不合法"
        ) String resourceType,
        @Positive(message = "客户等级不合法") Long customerLevelId,
        @DecimalMin(value = "0.00", message = "建议比例不能小于0")
        @Digits(integer = 6, fraction = 2, message = "建议比例最多6位整数和2位小数") BigDecimal suggestedRate,
        @DecimalMin(value = "0.00", message = "最低比例不能小于0")
        @Digits(integer = 6, fraction = 2, message = "最低比例最多6位整数和2位小数") BigDecimal minimumRate,
        @DecimalMin(value = "0.00", message = "建议固定加价不能小于0")
        @Digits(integer = 12, fraction = 2, message = "建议固定加价最多12位整数和2位小数") BigDecimal suggestedFixedAddon,
        @DecimalMin(value = "0.00", message = "最低固定加价不能小于0")
        @Digits(integer = 12, fraction = 2, message = "最低固定加价最多12位整数和2位小数") BigDecimal minimumFixedAddon,
        @Pattern(regexp = "active|disabled", message = "普通资源报价规则状态不合法") String status,
        String remark
) {
}
