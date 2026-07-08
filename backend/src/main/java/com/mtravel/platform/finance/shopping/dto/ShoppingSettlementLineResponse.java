package com.mtravel.platform.finance.shopping.dto;

import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementLineEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 购物结算明细响应。
 *
 * @param feedbackLineId 购物反馈明细 ID
 * @param shopName 购物店名称
 * @param businessDate 消费日期
 * @param peopleCount 进店人数
 * @param consumptionAmount 消费总额
 * @param companyRebateAmount 公司返佣金额
 * @param guideCommissionAmount 导游从购物店现场取得或应得的佣金金额，仅用于核对
 * @param headFeeAmount 人头费金额
 * @param lineCompanyProfitAmount 明细公司利润，按人头费加公司返佣计算
 */
public record ShoppingSettlementLineResponse(
        Long feedbackLineId,
        String shopName,
        LocalDate businessDate,
        Integer peopleCount,
        BigDecimal consumptionAmount,
        BigDecimal companyRebateAmount,
        BigDecimal guideCommissionAmount,
        BigDecimal headFeeAmount,
        BigDecimal lineCompanyProfitAmount
) {

    /** 根据结算明细实体生成响应。 */
    public static ShoppingSettlementLineResponse fromEntity(FinanceShoppingSettlementLineEntity entity) {
        return new ShoppingSettlementLineResponse(
                entity.getFeedbackLineId(),
                entity.getShopName(),
                entity.getBusinessDate(),
                entity.getPeopleCount(),
                entity.getConsumptionAmount(),
                entity.getCompanyRebateAmount(),
                entity.getGuideCommissionAmount(),
                entity.getHeadFeeAmount(),
                entity.getLineCompanyProfitAmount()
        );
    }
}
