package com.mtravel.platform.customer.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 新增客户分类请求。
 *
 * @param categoryName 分类名称，同一租户下未删除记录不可重复。
 * @param defaultCreditLimit 默认授信额度，单位元，允许为空，空值按 0 处理。
 * @param sortOrder 排序号，允许为空，为空时后端按 0 处理。
 * @param status 分类状态，只允许 active / disabled，允许为空，默认 active。
 * @param remark 备注，用于说明分类规则。
 */
public record CustomerCategoryCreateRequest(
        @NotBlank(message = "客户分类名称不能为空")
        @Size(max = 100, message = "客户分类名称不能超过100个字符")
        String categoryName,
        @DecimalMin(value = "0.00", message = "默认授信额度不能小于0")
        BigDecimal defaultCreditLimit,
        Integer sortOrder,
        @Pattern(regexp = "active|disabled", message = "客户分类状态只能是active或disabled")
        String status,
        String remark
) {
}
