package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 地接整团打包报价规则保存请求。
 */
public record SalesQuoteGroundAgentRuleSaveRequest(
        @NotNull(message = "最小人数不能为空")
        @Min(value = 1, message = "最小人数不能小于1")
        Integer minPeople,
        @NotNull(message = "最大人数不能为空")
        @Min(value = 1, message = "最大人数不能小于1")
        Integer maxPeople,
        @DecimalMin(value = "0", message = "整团打包价不能小于0")
        BigDecimal groupPackagePrice,
        @Pattern(regexp = "active|disabled", message = "规则状态不合法")
        String status,
        @Size(max = 1000, message = "备注最多1000个字符")
        String remark
) {}
