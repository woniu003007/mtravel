package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;

/** 产品设计中展示的供应商分类报价行。 */
public record ProductDesignerSupplierPriceLineResponse(
        Long resourceProjectId,
        String projectName,
        BigDecimal marketPrice,
        BigDecimal peerPrice,
        BigDecimal teamPrice
) {}
