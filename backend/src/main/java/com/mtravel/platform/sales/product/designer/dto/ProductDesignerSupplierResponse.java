package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;
import java.util.List;

/** 资源有效供应商绑定响应。 */
public record ProductDesignerSupplierResponse(
        Long relationId,
        Long supplierId,
        String supplierName,
        Boolean isDefault,
        String priceMode,
        BigDecimal unifiedPrice,
        BigDecimal referenceUnitPrice,
        List<ProductDesignerSupplierPriceLineResponse> priceLines
) {}
