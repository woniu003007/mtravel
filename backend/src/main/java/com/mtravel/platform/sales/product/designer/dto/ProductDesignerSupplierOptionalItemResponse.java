package com.mtravel.platform.sales.product.designer.dto;
import java.math.BigDecimal;
/** 产品设计选择供应商时可选的自费项目成本与建议售价。 */
public record ProductDesignerSupplierOptionalItemResponse(Long id, Long resourceOptionalItemId, String projectName, BigDecimal costPrice, BigDecimal suggestedSalePrice, String status) {}
