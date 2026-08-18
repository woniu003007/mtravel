package com.mtravel.platform.configuration.quote.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 导游等级保存请求。
 */
public record SalesQuoteGuideLevelSaveRequest(
        @NotBlank(message = "导游等级不能为空")
        @Size(max = 80, message = "导游等级最多80个字符")
        String levelName,
        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,
        @Pattern(regexp = "active|disabled", message = "等级状态不合法")
        String status,
        @Size(max = 1000, message = "备注最多1000个字符")
        String remark
) {}
