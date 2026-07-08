package com.mtravel.platform.finance.shopping.dto;

import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 购物佣金结算响应。
 *
 * @param id 结算快照 ID
 * @param teamId 团队 ID
 * @param teamNo 团号
 * @param ruleSource 规则来源
 * @param guestCount 团队实收人数
 * @param thresholdPerCapitaAmount 人均消费门槛
 * @param baseCommissionRate 基础佣金比例
 * @param targetCommissionRate 达标后目标佣金比例
 * @param totalConsumptionAmount 消费总额
 * @param perCapitaConsumptionAmount 人均消费
 * @param thresholdReached 是否达标
 * @param baseGuideCommissionAmount 基础导游佣金
 * @param ladderExtraCommissionAmount 参考阶梯补差佣金
 * @param guideCommissionTotalAmount 导游现场佣金和参考补差展示合计
 * @param manualGuideBonusAmount 计调确认由公司补给导游的正式补佣金额
 * @param manualGuideBonusRemark 公司补佣说明
 * @param companyRebateAmount 公司返佣合计
 * @param headFeeAmount 人头费合计
 * @param companyProfitAmount 内账公司利润
 * @param externalCompanyProfitAmount 外账公司利润
 * @param calculatedBy 计算人
 * @param calculatedAt 计算时间
 * @param lines 结算明细
 */
public record ShoppingSettlementResponse(
        Long id,
        Long teamId,
        String teamNo,
        String ruleSource,
        Integer guestCount,
        BigDecimal thresholdPerCapitaAmount,
        BigDecimal baseCommissionRate,
        BigDecimal targetCommissionRate,
        BigDecimal totalConsumptionAmount,
        BigDecimal perCapitaConsumptionAmount,
        Boolean thresholdReached,
        BigDecimal baseGuideCommissionAmount,
        BigDecimal ladderExtraCommissionAmount,
        BigDecimal guideCommissionTotalAmount,
        BigDecimal manualGuideBonusAmount,
        String manualGuideBonusRemark,
        BigDecimal companyRebateAmount,
        BigDecimal headFeeAmount,
        BigDecimal companyProfitAmount,
        BigDecimal externalCompanyProfitAmount,
        String calculatedBy,
        OffsetDateTime calculatedAt,
        List<ShoppingSettlementLineResponse> lines
) {

    /** 根据结算实体生成响应。 */
    public static ShoppingSettlementResponse fromEntity(
            FinanceShoppingSettlementEntity entity,
            List<ShoppingSettlementLineResponse> lines
    ) {
        return new ShoppingSettlementResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getTeamNo(),
                entity.getRuleSource(),
                entity.getGuestCount(),
                entity.getThresholdPerCapitaAmount(),
                entity.getBaseCommissionRate(),
                entity.getTargetCommissionRate(),
                entity.getTotalConsumptionAmount(),
                entity.getPerCapitaConsumptionAmount(),
                entity.getThresholdReached(),
                entity.getBaseGuideCommissionAmount(),
                entity.getLadderExtraCommissionAmount(),
                entity.getGuideCommissionTotalAmount(),
                entity.getManualGuideBonusAmount(),
                entity.getManualGuideBonusRemark(),
                entity.getCompanyRebateAmount(),
                entity.getHeadFeeAmount(),
                entity.getInternalCompanyProfitAmount(),
                entity.getExternalCompanyProfitAmount(),
                entity.getCalculatedBy(),
                entity.getCalculatedAt(),
                lines
        );
    }
}
