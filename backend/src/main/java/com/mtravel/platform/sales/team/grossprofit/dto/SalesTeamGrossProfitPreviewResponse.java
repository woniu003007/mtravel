package com.mtravel.platform.sales.team.grossprofit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 团队预算毛利表预览响应。
 *
 * <p>该 DTO 同时支撑前端预览、Word 导出和 PDF 导出，金额口径按旧系统
 * `团队毛利表(预算)`：订单收入 + 自费公司利润 + 购物公司利润 - 普通团队成本 - 导服费。</p>
 */
public record SalesTeamGrossProfitPreviewResponse(
        TeamInfo team,
        List<IncomeRow> incomeRows,
        List<CostRow> costRows,
        List<OptionalRow> optionalRows,
        List<ShoppingRow> shoppingRows,
        Summary summary,
        List<SalespersonSummary> salespersonRows
) {
    /** 团队抬头信息。 */
    public record TeamInfo(
            Long teamId,
            String productName,
            String teamNo,
            LocalDate departureDate,
            Integer travelDays,
            Integer guestCount,
            String guideSummary,
            String operatorName
    ) {
    }

    /** 收入明细行。 */
    public record IncomeRow(
            String customerName,
            String salespersonName,
            Integer guestCount,
            String receivableDetail,
            BigDecimal receivableAmount,
            BigDecimal receivedAmount,
            String bookingOperatorName
    ) {
    }

    /** 普通团队成本支出行，不包含自费和购物。 */
    public record CostRow(
            String category,
            String supplierName,
            String costDescription,
            BigDecimal payableAmount,
            BigDecimal cashAmount,
            BigDecimal paidCreditAmount,
            String auditorName
    ) {
    }

    /** 自费项目公司利润行。 */
    public record OptionalRow(
            String projectName,
            BigDecimal guestCount,
            BigDecimal salesAmount,
            BigDecimal costAmount,
            BigDecimal guideCommissionAmount,
            BigDecimal companyProfit,
            String auditorName
    ) {
    }

    /** 购物项目公司利润行。 */
    public record ShoppingRow(
            String shopName,
            BigDecimal entryCount,
            BigDecimal headFeeAmount,
            BigDecimal consumptionAmount,
            BigDecimal companyRebateAmount,
            BigDecimal guideCommissionAmount,
            BigDecimal companyProfit,
            String auditorName
    ) {
    }

    /** 团队毛利汇总。 */
    public record Summary(
            BigDecimal orderIncome,
            BigDecimal shoppingProfit,
            BigDecimal optionalProfit,
            BigDecimal regularCost,
            BigDecimal guideFee,
            BigDecimal grossProfit
    ) {
    }

    /** 业务员明细汇总。 */
    public record SalespersonSummary(
            String salespersonName,
            BigDecimal receivableAmount,
            BigDecimal receivedAmount,
            BigDecimal grossProfit,
            BigDecimal grossProfitRate
    ) {
    }
}
