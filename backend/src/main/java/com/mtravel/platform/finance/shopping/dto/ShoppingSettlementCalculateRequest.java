package com.mtravel.platform.finance.shopping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 购物业绩结算计算请求。
 *
 * @param manualGuideBonusAmount 计调确认由公司补给导游的正式补佣金额
 * @param manualGuideBonusRemark 公司补佣说明
 */
public record ShoppingSettlementCalculateRequest(
        @DecimalMin(value = "0", message = "公司补佣不能小于0") BigDecimal manualGuideBonusAmount,
        @Size(max = 1000, message = "公司补佣说明不能超过1000字") String manualGuideBonusRemark
) {
}
