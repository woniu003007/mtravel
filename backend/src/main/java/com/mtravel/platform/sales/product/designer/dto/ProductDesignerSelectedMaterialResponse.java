package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;

/** 产品每日资源中已保存的统一素材顺序及其展示快照。 */
public record ProductDesignerSelectedMaterialResponse(
        String materialType,
        Long introductionId,
        Long resourceOptionalItemId,
        Long supplierOptionalItemId,
        BigDecimal salePrice,
        Integer sortOrder,
        String title,
        String content,
        String projectName
) {}
