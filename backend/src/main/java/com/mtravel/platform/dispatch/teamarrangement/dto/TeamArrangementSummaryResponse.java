package com.mtravel.platform.dispatch.teamarrangement.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 正式团队安排金额汇总响应。
 *
 * <p>该响应专供团队安排页展示成本总览、订单收款汇总和预算利润。金额由后端统一计算，
 * 前端不能把这些字段重新按页面数组聚合成财务或经营结果。</p>
 */
public record TeamArrangementSummaryResponse(
        BigDecimal orderReceivableAmount,
        BigDecimal orderReceivedAmount,
        BigDecimal orderBalanceAmount,
        BigDecimal regularCostAmount,
        BigDecimal optionalCompanyProfitAmount,
        BigDecimal shoppingCompanyProfitAmount,
        BigDecimal guideFeeAmount,
        BigDecimal guideOperationFeeAmount,
        BigDecimal guideImprestAmount,
        BigDecimal budgetProfitAmount,
        List<CostColumn> costColumns,
        List<SectionSummary> sectionSummaries
) {
    /**
     * 成本总览列汇总。
     *
     * @param key 前端固定资源类型或合计项 key
     * @param label 展示名称
     * @param cashAmount 现结金额
     * @param creditAmount 挂账金额
     */
    public record CostColumn(
            String key,
            String label,
            BigDecimal cashAmount,
            BigDecimal creditAmount
    ) {
    }

    /**
     * 单个安排分类小计。
     *
     * @param arrangementType 安排分类
     * @param count 已保存记录数
     * @param costAmount 成本或金额合计
     * @param cashAmount 现结金额
     * @param creditAmount 挂账金额
     */
    public record SectionSummary(
            String arrangementType,
            long count,
            BigDecimal costAmount,
            BigDecimal cashAmount,
            BigDecimal creditAmount
    ) {
    }
}
