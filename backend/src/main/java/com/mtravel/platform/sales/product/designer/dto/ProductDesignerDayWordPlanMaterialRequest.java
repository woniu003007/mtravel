package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 产品当天景区 Word 方案中的一个统一排序素材。 */
public record ProductDesignerDayWordPlanMaterialRequest(
        @NotNull Long dayResourceId,
        @NotBlank String materialType,
        Long introductionId,
        Long resourceOptionalItemId,
        Long supplierOptionalItemId,
        @DecimalMin(value = "0", inclusive = false, message = "自费项目对外价必须大于0") BigDecimal salePrice
) {}
