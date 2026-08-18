package com.mtravel.platform.purchase.resource.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 资源页快捷新增供应商时提交的报价明细。
 *
 * @param resourceProjectId 资源费用项目 ID
 * @param teamPrice 供应商报价，后续产品估价优先读取该价格
 * @param priceDescription 报价说明
 */
public record ResourceSupplierPriceLineRequest(
        @NotNull(message = "报价项目不能为空") Long resourceProjectId,
        @DecimalMin(value = "0.00", message = "报价不能小于0") BigDecimal teamPrice,
        @Size(max = 500, message = "报价说明不能超过500个字符") String priceDescription
) {}
