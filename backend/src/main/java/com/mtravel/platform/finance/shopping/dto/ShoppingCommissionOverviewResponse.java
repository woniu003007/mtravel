package com.mtravel.platform.finance.shopping.dto;

import java.util.List;

/**
 * 团队购物佣金总览响应。
 *
 * @param rule 当前有效规则
 * @param feedbackLines 购物店反馈明细
 * @param latestSettlement 最新有效结算快照
 */
public record ShoppingCommissionOverviewResponse(
        ShoppingCommissionRuleResponse rule,
        List<ShoppingFeedbackLineResponse> feedbackLines,
        ShoppingSettlementResponse latestSettlement
) {
}
