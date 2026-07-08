package com.mtravel.platform.finance.guideimprest.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 导游备用金计算预览响应。
 *
 * @param teamId 团队 ID
 * @param teamNo 团号
 * @param guideId 导游 ID
 * @param guideName 导游姓名
 * @param guestCount 团队实收人数
 * @param companyMarkupRate 公司规定加点率
 * @param cashCostAmount 现付总成本
 * @param optionalDeductionAmount 自费加点抵扣金额
 * @param calculatedAmount 原始计算结果，可为负数
 * @param suggestedImprestAmount 建议发放备用金，负数时为 0
 * @param guideTurnInAmount 计算为负数时导游应上交金额
 * @param occupiedAuthorizationAmount 已占用授权金额，包含待审批、已审批和已付款未作废申请
 * @param availableAuthorizationAmount 当前还可申请授权金额
 * @param calcLines 计算明细
 */
public record GuideImprestPreviewResponse(
        Long teamId,
        String teamNo,
        Long guideId,
        String guideName,
        Integer guestCount,
        BigDecimal companyMarkupRate,
        BigDecimal cashCostAmount,
        BigDecimal optionalDeductionAmount,
        BigDecimal calculatedAmount,
        BigDecimal suggestedImprestAmount,
        BigDecimal guideTurnInAmount,
        BigDecimal occupiedAuthorizationAmount,
        BigDecimal availableAuthorizationAmount,
        List<GuideImprestCalcLineResponse> calcLines
) {
}
