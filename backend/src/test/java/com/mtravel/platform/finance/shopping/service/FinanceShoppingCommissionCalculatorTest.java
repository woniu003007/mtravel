package com.mtravel.platform.finance.shopping.service;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 购物阶梯佣金计算测试。
 *
 * <p>购物佣金和公司利润属于敏感经营金额，必须由后端按规则统一计算，
 * 前端只能展示计算结果，不能把页面输入直接当成最终结算金额。</p>
 */
class FinanceShoppingCommissionCalculatorTest {

    private final FinanceShoppingCommissionCalculator calculator = new FinanceShoppingCommissionCalculator();

    @Test
    void shouldCalculateFullAmountLadderExtraWhenTeamPerCapitaReachesThreshold() {
        FinanceShoppingCommissionCalculator.RuleSnapshot rule = rule("5000.00", "8.00", "10.00");
        List<FinanceShoppingCommissionCalculator.FeedbackAmount> feedback = List.of(
                feedback(11L, "丝绸购物店", "100000.00", "10000.00", "8000.00", "2000.00", 50),
                feedback(12L, "珠宝购物店", "200000.00", "20000.00", "16000.00", "3000.00", 50)
        );

        FinanceShoppingCommissionCalculator.Result result = calculator.calculate(50, rule, feedback);

        assertThat(result.totalConsumptionAmount()).isEqualByComparingTo("300000.00");
        assertThat(result.perCapitaConsumptionAmount()).isEqualByComparingTo("6000.00");
        assertThat(result.thresholdReached()).isTrue();
        assertThat(result.baseGuideCommissionAmount()).isEqualByComparingTo("24000.00");
        assertThat(result.ladderExtraCommissionAmount()).isEqualByComparingTo("6000.00");
        assertThat(result.guideCommissionTotalAmount()).isEqualByComparingTo("30000.00");
        assertThat(result.companyProfitAmount()).isEqualByComparingTo("35000.00");
    }

    @Test
    void shouldOnlySubtractManualCompanyBonusFromInternalProfit() {
        FinanceShoppingCommissionCalculator.RuleSnapshot rule = rule("5000.00", "8.00", "10.00");
        List<FinanceShoppingCommissionCalculator.FeedbackAmount> feedback = List.of(
                feedback(11L, "丝绸购物店", "100000.00", "10000.00", "8000.00", "2000.00", 50),
                feedback(12L, "珠宝购物店", "200000.00", "20000.00", "16000.00", "3000.00", 50)
        );

        FinanceShoppingCommissionCalculator.Result result = calculator.calculate(
                50,
                rule,
                feedback,
                new BigDecimal("9000.00")
        );

        assertThat(result.ladderExtraCommissionAmount()).isEqualByComparingTo("6000.00");
        assertThat(result.guideCommissionTotalAmount()).isEqualByComparingTo("30000.00");
        assertThat(result.companyProfitAmount()).isEqualByComparingTo("26000.00");
    }

    @Test
    void shouldNotCalculateLadderExtraWhenPerCapitaIsBelowThreshold() {
        FinanceShoppingCommissionCalculator.RuleSnapshot rule = rule("5000.00", "8.00", "10.00");
        List<FinanceShoppingCommissionCalculator.FeedbackAmount> feedback = List.of(
                feedback(11L, "丝绸购物店", "80000.00", "8000.00", "6400.00", "2000.00", 50),
                feedback(12L, "珠宝购物店", "120000.00", "12000.00", "9600.00", "3000.00", 50)
        );

        FinanceShoppingCommissionCalculator.Result result = calculator.calculate(50, rule, feedback);

        assertThat(result.totalConsumptionAmount()).isEqualByComparingTo("200000.00");
        assertThat(result.perCapitaConsumptionAmount()).isEqualByComparingTo("4000.00");
        assertThat(result.thresholdReached()).isFalse();
        assertThat(result.ladderExtraCommissionAmount()).isEqualByComparingTo("0.00");
        assertThat(result.guideCommissionTotalAmount()).isEqualByComparingTo("16000.00");
        assertThat(result.companyProfitAmount()).isEqualByComparingTo("25000.00");
    }

    @Test
    void shouldUseZeroPerCapitaAndNoExtraWhenGuestCountIsZero() {
        FinanceShoppingCommissionCalculator.RuleSnapshot rule = rule("5000.00", "8.00", "10.00");
        List<FinanceShoppingCommissionCalculator.FeedbackAmount> feedback = List.of(
                feedback(11L, "丝绸购物店", "100000.00", "10000.00", "8000.00", "2000.00", 0)
        );

        FinanceShoppingCommissionCalculator.Result result = calculator.calculate(0, rule, feedback);

        assertThat(result.perCapitaConsumptionAmount()).isEqualByComparingTo("0.00");
        assertThat(result.thresholdReached()).isFalse();
        assertThat(result.ladderExtraCommissionAmount()).isEqualByComparingTo("0.00");
        assertThat(result.companyProfitAmount()).isEqualByComparingTo("12000.00");
    }

    private static FinanceShoppingCommissionCalculator.RuleSnapshot rule(
            String threshold,
            String baseRate,
            String targetRate
    ) {
        return new FinanceShoppingCommissionCalculator.RuleSnapshot(
                new BigDecimal(threshold),
                new BigDecimal(baseRate),
                new BigDecimal(targetRate),
                "full_amount_diff"
        );
    }

    private static FinanceShoppingCommissionCalculator.FeedbackAmount feedback(
            Long sourceId,
            String shopName,
            String consumptionAmount,
            String companyRebateAmount,
            String guideCommissionAmount,
            String headFeeAmount,
            Integer peopleCount
    ) {
        return new FinanceShoppingCommissionCalculator.FeedbackAmount(
                sourceId,
                shopName,
                new BigDecimal(consumptionAmount),
                new BigDecimal(companyRebateAmount),
                new BigDecimal(guideCommissionAmount),
                new BigDecimal(headFeeAmount),
                peopleCount
        );
    }
}
