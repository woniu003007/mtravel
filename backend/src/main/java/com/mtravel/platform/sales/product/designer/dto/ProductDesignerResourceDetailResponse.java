package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;
import java.util.List;

/** 地图点选后的资源详情响应，包含已发布介绍、图片和有效供应商绑定。 */
public record ProductDesignerResourceDetailResponse(
        Long id,
        String resourceType,
        String procurementMode,
        String resourceName,
        String province,
        String city,
        String district,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String introduction,
        String warmTip,
        List<ProductDesignerIntroductionResponse> introductions,
        List<ProductDesignerResourceImageResponse> images,
        List<ProductDesignerSupplierResponse> suppliers,
        Long defaultSupplierId
) {}
