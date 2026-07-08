package com.mtravel.platform.finance.shopping.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * 团队购物参考阶梯规则保存请求。
 *
 * @param thresholdPerCapitaAmount 人均消费门槛
 * @param baseCommissionRate 基础佣金比例，百分数
 * @param targetCommissionRate 达标后目标佣金比例，百分数
 * @param overrideReason 团队覆盖原因
 */
public record ShoppingCommissionRuleSaveRequest(
        @DecimalMin(value = "0", message = "人均门槛不能小于0") BigDecimal thresholdPerCapitaAmount,
        @DecimalMin(value = "0", message = "基础佣金比例不能小于0") BigDecimal baseCommissionRate,
        @DecimalMin(value = "0", message = "目标佣金比例不能小于0") BigDecimal targetCommissionRate,
        String overrideReason
) {
}
