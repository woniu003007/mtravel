package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 普通资源销售报价规则保存请求。
 */
public record SalesQuoteResourceRuleSaveRequest(
        @Pattern(regexp = "scenic|hotel|restaurant|vehicle|transport|other|misc", message = "报价资源类型不合法")
        String resourceType,
        Long customerCategoryId,
        @Pattern(regexp = "rate|fixed|both", message = "报价方式不合法")
        String quoteMode,
        @DecimalMin(value = "0", message = "建议比例上浮不能小于0")
        BigDecimal suggestedMarkupRate,
        @DecimalMin(value = "0", message = "最低比例上浮不能小于0")
        BigDecimal minimumMarkupRate,
        @DecimalMin(value = "0", message = "建议固定加价不能小于0")
        BigDecimal suggestedFixedMarkup,
        @DecimalMin(value = "0", message = "最低固定加价不能小于0")
        BigDecimal minimumFixedMarkup,
        @Pattern(regexp = "active|disabled", message = "规则状态不合法")
        String status,
        @Size(max = 1000, message = "备注最多1000个字符")
        String remark
) {}
