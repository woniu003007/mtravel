package com.mtravel.platform.finance.shopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 购物业绩和补佣计算器。
 *
 * <p>该类只负责纯金额公式，不访问数据库。导游从购物店现场取得的佣金只作为核对金额，
 * 不作为公司成本扣减；计调确认的公司补佣才进入内部利润扣减。</p>
 */
@Component
public class FinanceShoppingCommissionCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String FULL_AMOUNT_DIFF = "full_amount_diff";

    /**
     * 计算购物业绩参考佣金和公司利润，未传公司补佣时按 0 处理。
     *
     * @param guestCount 团队实收人数
     * @param rule 计算规则快照
     * @param feedbackAmounts 购物店反馈金额明细
     * @return 团队购物佣金计算结果
     */
    public Result calculate(Integer guestCount, RuleSnapshot rule, List<FeedbackAmount> feedbackAmounts) {
        return calculate(guestCount, rule, feedbackAmounts, ZERO);
    }

    /**
     * 计算购物业绩参考佣金和公司利润。
     *
     * @param guestCount 团队实收人数
     * @param rule 计算规则快照
     * @param feedbackAmounts 购物店反馈金额明细
     * @param manualCompanyBonusAmount 计调确认由公司补给导游的正式补佣金额
     * @return 团队购物佣金计算结果
     */
    public Result calculate(
            Integer guestCount,
            RuleSnapshot rule,
            List<FeedbackAmount> feedbackAmounts,
            BigDecimal manualCompanyBonusAmount
    ) {
        List<FeedbackAmount> safeFeedback = feedbackAmounts == null ? List.of() : feedbackAmounts;
        BigDecimal totalConsumption = sum(safeFeedback, FeedbackAmount::consumptionAmount);
        BigDecimal companyRebate = sum(safeFeedback, FeedbackAmount::companyRebateAmount);
        BigDecimal directGuideCommission = sum(safeFeedback, FeedbackAmount::guideCommissionAmount);
        BigDecimal headFee = sum(safeFeedback, FeedbackAmount::headFeeAmount);
        int safeGuestCount = Math.max(guestCount == null ? 0 : guestCount, 0);
        BigDecimal perCapita = safeGuestCount > 0
                ? totalConsumption.divide(BigDecimal.valueOf(safeGuestCount), 2, RoundingMode.HALF_UP)
                : ZERO;
        boolean thresholdReached = safeGuestCount > 0
                && perCapita.compareTo(money(rule.thresholdPerCapitaAmount())) >= 0
                && totalConsumption.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal baseCommission = rateAmount(totalConsumption, rule.baseCommissionRate());
        BigDecimal ladderExtra = thresholdReached
                ? calculateLadderExtra(totalConsumption, rule)
                : ZERO;
        BigDecimal guideCommissionTotal = directGuideCommission.add(ladderExtra).setScale(2, RoundingMode.HALF_UP);
        BigDecimal manualCompanyBonus = money(manualCompanyBonusAmount);
        BigDecimal companyProfit = headFee
                .add(companyRebate)
                .subtract(manualCompanyBonus)
                .setScale(2, RoundingMode.HALF_UP);
        List<LineResult> lines = safeFeedback.stream()
                .map(item -> new LineResult(
                        item.sourceId(),
                        item.shopName(),
                        money(item.consumptionAmount()),
                        money(item.companyRebateAmount()),
                        money(item.guideCommissionAmount()),
                        money(item.headFeeAmount()),
                        item.peopleCount()
                ))
                .toList();
        return new Result(
                totalConsumption.setScale(2, RoundingMode.HALF_UP),
                perCapita,
                thresholdReached,
                baseCommission,
                ladderExtra,
                guideCommissionTotal,
                companyRebate.setScale(2, RoundingMode.HALF_UP),
                headFee.setScale(2, RoundingMode.HALF_UP),
                companyProfit,
                lines
        );
    }

    private BigDecimal calculateLadderExtra(BigDecimal totalConsumption, RuleSnapshot rule) {
        BigDecimal diffRate = money(rule.targetCommissionRate()).subtract(money(rule.baseCommissionRate()));
        if (diffRate.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        if (!FULL_AMOUNT_DIFF.equals(rule.ladderCalcMode())) {
            return ZERO;
        }
        return rateAmount(totalConsumption, diffRate);
    }

    private BigDecimal rateAmount(BigDecimal amount, BigDecimal ratePercent) {
        return money(amount)
                .multiply(money(ratePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<FeedbackAmount> items, AmountExtractor extractor) {
        return items.stream()
                .map(extractor::amount)
                .map(FinanceShoppingCommissionCalculator::money)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private interface AmountExtractor {
        BigDecimal amount(FeedbackAmount item);
    }

    /**
     * 购物补佣参考规则快照。
     *
     * @param thresholdPerCapitaAmount 人均消费门槛
     * @param baseCommissionRate 基础导游佣金比例，百分数
     * @param targetCommissionRate 达标后目标佣金比例，百分数
     * @param ladderCalcMode 阶梯补差方式，full_amount_diff 表示达标后按全额补差
     */
    public record RuleSnapshot(
            BigDecimal thresholdPerCapitaAmount,
            BigDecimal baseCommissionRate,
            BigDecimal targetCommissionRate,
            String ladderCalcMode
    ) {
    }

    /**
     * 单条购物店反馈金额。
     *
     * @param sourceId 来源明细 ID
     * @param shopName 购物店名称
     * @param consumptionAmount 消费总额
     * @param companyRebateAmount 公司返佣金额
     * @param guideCommissionAmount 导游从购物店现场取得或应得的佣金，仅用于核对
     * @param headFeeAmount 人头费金额
     * @param peopleCount 进店人数
     */
    public record FeedbackAmount(
            Long sourceId,
            String shopName,
            BigDecimal consumptionAmount,
            BigDecimal companyRebateAmount,
            BigDecimal guideCommissionAmount,
            BigDecimal headFeeAmount,
            Integer peopleCount
    ) {
    }

    /**
     * 团队购物业绩计算结果。
     *
     * @param totalConsumptionAmount 全团购物消费总额
     * @param perCapitaConsumptionAmount 按团队实收人数计算的人均消费
     * @param thresholdReached 是否达到阶梯门槛
     * @param baseGuideCommissionAmount 按基础比例测算的参考导游佣金
     * @param ladderExtraCommissionAmount 达标后参考补差佣金，不自动计入正式成本
     * @param guideCommissionTotalAmount 导游现场佣金加参考补差的展示合计
     * @param companyRebateAmount 公司返佣合计
     * @param headFeeAmount 人头费合计
     * @param companyProfitAmount 公司内部购物利润
     * @param lines 反馈明细计算快照
     */
    public record Result(
            BigDecimal totalConsumptionAmount,
            BigDecimal perCapitaConsumptionAmount,
            Boolean thresholdReached,
            BigDecimal baseGuideCommissionAmount,
            BigDecimal ladderExtraCommissionAmount,
            BigDecimal guideCommissionTotalAmount,
            BigDecimal companyRebateAmount,
            BigDecimal headFeeAmount,
            BigDecimal companyProfitAmount,
            List<LineResult> lines
    ) {
    }

    /**
     * 购物反馈行结果。
     */
    public record LineResult(
            Long sourceId,
            String shopName,
            BigDecimal consumptionAmount,
            BigDecimal companyRebateAmount,
            BigDecimal guideCommissionAmount,
            BigDecimal headFeeAmount,
            Integer peopleCount
    ) {
    }
}
