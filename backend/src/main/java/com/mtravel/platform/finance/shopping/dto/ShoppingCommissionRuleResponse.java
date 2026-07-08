package com.mtravel.platform.finance.shopping.dto;

import com.mtravel.platform.finance.shopping.entity.FinanceShoppingTeamRuleOverrideEntity;
import java.math.BigDecimal;

/**
 * 购物参考阶梯规则响应。
 *
 * @param ruleSource 规则来源
 * @param thresholdPerCapitaAmount 人均消费门槛
 * @param baseCommissionRate 基础佣金比例
 * @param targetCommissionRate 达标后目标佣金比例
 * @param ladderCalcMode 参考补差测算方式
 * @param overrideReason 团队覆盖原因
 */
public record ShoppingCommissionRuleResponse(
        String ruleSource,
        BigDecimal thresholdPerCapitaAmount,
        BigDecimal baseCommissionRate,
        BigDecimal targetCommissionRate,
        String ladderCalcMode,
        String overrideReason
) {

    /** 根据团队覆盖实体生成响应。 */
    public static ShoppingCommissionRuleResponse fromOverride(FinanceShoppingTeamRuleOverrideEntity entity) {
        return new ShoppingCommissionRuleResponse(
                "team_override",
                entity.getThresholdPerCapitaAmount(),
                entity.getBaseCommissionRate(),
                entity.getTargetCommissionRate(),
                entity.getLadderCalcMode(),
                entity.getOverrideReason()
        );
    }
}
