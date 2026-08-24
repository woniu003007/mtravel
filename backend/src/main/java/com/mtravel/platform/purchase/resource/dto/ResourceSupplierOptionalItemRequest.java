package com.mtravel.platform.purchase.resource.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 资源页供应商绑定下的自费项目报价请求。
 *
 * <p>自费项目统一按元/人计价，不允许前端传入其它计价单位。</p>
 */
public record ResourceSupplierOptionalItemRequest(
        Long resourceOptionalItemId,
        @NotBlank(message = "自费项目名称不能为空")
        @Size(max = 200, message = "自费项目名称不能超过200个字符")
        String projectName,

        @NotNull(message = "自费项目供应商成本价不能为空")
        @DecimalMin(value = "0.00", message = "自费项目供应商成本价不能小于0")
        BigDecimal costPrice,

        @DecimalMin(value = "0.00", message = "建议对外自费价不能小于0")
        BigDecimal suggestedSalePrice,

        @Size(max = 500, message = "自费项目价格说明不能超过500个字符")
        String priceDescription,

        @Pattern(regexp = "active|disabled", message = "自费项目状态不合法")
        String status
) {
    /** 兼容旧客户端仅提交项目名称、成本和说明的请求。 */
    public ResourceSupplierOptionalItemRequest(String projectName, BigDecimal costPrice, String priceDescription, String status) {
        this(null, projectName, costPrice, null, priceDescription, status);
    }
}
