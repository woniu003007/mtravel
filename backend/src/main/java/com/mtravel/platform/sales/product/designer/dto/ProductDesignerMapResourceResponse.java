package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;

/** 资源池列表响应，只返回摘要；经纬度为空时表示尚未完成地图点位。 */
public record ProductDesignerMapResourceResponse(
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
        String status,
        Long defaultRelationId,
        Long defaultSupplierId,
        String defaultSupplierName,
        BigDecimal referenceUnitPrice
) {}
