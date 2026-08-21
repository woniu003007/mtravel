package com.mtravel.platform.sales.product.designer.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
/** 产品中选择的一个自费项目；成本字段仅兼容旧端，保存时不信任并由后端重取。 */
public record ProductDesignerSelectedOptionalItemRequest(
        @NotNull Long resourceOptionalItemId, Long introductionId, Long supplierOptionalItemId,
        BigDecimal costPrice, BigDecimal suggestedSalePrice,
        @DecimalMin(value = "0", message = "自费项目对外价不能小于0") BigDecimal salePrice
) {}
