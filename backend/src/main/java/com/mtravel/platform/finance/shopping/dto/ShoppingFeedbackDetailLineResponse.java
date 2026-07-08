package com.mtravel.platform.finance.shopping.dto;

import com.mtravel.platform.finance.shopping.entity.FinanceShoppingFeedbackDetailLineEntity;
import java.math.BigDecimal;

/**
 * 购物反馈消费详情响应。
 *
 * @param id 明细 ID
 * @param categoryName 购物品类
 * @param peopleCount 当前品类进店人数，用于核对
 * @param headFeeAmount 人头费金额
 * @param consumptionAmount 消费金额
 * @param companyRebateRate 公司返佣比例
 * @param companyRebateAmount 公司返佣金额
 * @param guideCommissionRate 导游现场提成比例
 * @param guideCommissionAmount 导游现场提成金额，仅用于核对
 * @param cashAmount 购物店现场现结金额，仅用于核对
 * @param remark 备注
 * @param sortOrder 排序号
 */
public record ShoppingFeedbackDetailLineResponse(
        Long id,
        String categoryName,
        Integer peopleCount,
        BigDecimal headFeeAmount,
        BigDecimal consumptionAmount,
        BigDecimal companyRebateRate,
        BigDecimal companyRebateAmount,
        BigDecimal guideCommissionRate,
        BigDecimal guideCommissionAmount,
        BigDecimal cashAmount,
        String remark,
        Integer sortOrder
) {

    /** 根据消费详情实体生成响应。 */
    public static ShoppingFeedbackDetailLineResponse fromEntity(FinanceShoppingFeedbackDetailLineEntity entity) {
        return new ShoppingFeedbackDetailLineResponse(
                entity.getId(),
                entity.getCategoryName(),
                entity.getPeopleCount(),
                entity.getHeadFeeAmount(),
                entity.getConsumptionAmount(),
                entity.getCompanyRebateRate(),
                entity.getCompanyRebateAmount(),
                entity.getGuideCommissionRate(),
                entity.getGuideCommissionAmount(),
                entity.getCashAmount(),
                entity.getRemark(),
                entity.getSortOrder()
        );
    }
}
