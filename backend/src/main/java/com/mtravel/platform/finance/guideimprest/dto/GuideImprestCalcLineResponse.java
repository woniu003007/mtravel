package com.mtravel.platform.finance.guideimprest.dto;

import java.math.BigDecimal;

/**
 * 导游备用金计算明细响应。
 *
 * @param lineType 明细类型，cash_cost 现付成本，optional_deduction 自费加点抵扣
 * @param arrangementType 团队安排类型
 * @param itemName 项目名称
 * @param salePrice 自费售价
 * @param costPrice 自费成本
 * @param guideCommissionAmount 导游提成金额
 * @param guideCommissionRate 导游提成比例
 * @param guideCommissionCalcType 导游提成计算方式，fixed 固定金额，percent 毛利百分比
 * @param companyMarkupRate 公司规定加点率
 * @param guestCount 团队实收人数
 * @param amount 本行金额
 * @param sortOrder 排序号
 */
public record GuideImprestCalcLineResponse(
        String lineType,
        String arrangementType,
        String itemName,
        BigDecimal salePrice,
        BigDecimal costPrice,
        BigDecimal guideCommissionAmount,
        BigDecimal guideCommissionRate,
        String guideCommissionCalcType,
        BigDecimal companyMarkupRate,
        Integer guestCount,
        BigDecimal amount,
        Integer sortOrder
) {
}
