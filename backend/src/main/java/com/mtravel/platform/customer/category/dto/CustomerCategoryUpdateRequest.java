package com.mtravel.platform.customer.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 修改客户分类请求。
 *
 * <p>修改时仍然要求传入完整的分类核心字段，避免前端漏传导致状态或排序被误清空。</p>
 */
public record CustomerCategoryUpdateRequest(
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
