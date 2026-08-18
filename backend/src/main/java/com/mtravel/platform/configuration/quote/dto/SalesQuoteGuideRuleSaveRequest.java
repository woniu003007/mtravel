package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 导游销售报价规则保存请求。
 */
public record SalesQuoteGuideRuleSaveRequest(
        @NotNull(message = "导游等级不能为空")
        Long guideLevelId,
        @Size(max = 80, message = "服务语种最多80个字符")
        String language,
        @DecimalMin(value = "0", message = "基础导服费不能小于0")
        BigDecimal baseDailyFee,
        @DecimalMin(value = "0", message = "外语服务加价不能小于0")
        BigDecimal foreignLanguageDailyMarkup,
        @DecimalMin(value = "0", message = "超时费不能小于0")
        BigDecimal overtimeHourlyFee,
        @Pattern(regexp = "active|disabled", message = "规则状态不合法")
        String status,
        @Size(max = 1000, message = "备注最多1000个字符")
        String remark
) {}
