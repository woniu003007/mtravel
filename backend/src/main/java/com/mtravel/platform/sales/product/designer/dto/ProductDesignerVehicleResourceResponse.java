package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;

/** 全程用车选择器的资源摘要，不包含地图坐标。 */
public record ProductDesignerVehicleResourceResponse(
        Long id,
        String resourceName,
        String province,
        String city,
        String vehicleType,
        Integer seatCount,
        String billingMode,
        String procurementMode
) {
    /** 从启用的用车资源主档生成选择器摘要。 */
    public static ProductDesignerVehicleResourceResponse fromEntity(PurchaseResourceEntity entity) {
        return new ProductDesignerVehicleResourceResponse(
                entity.getId(),
                entity.getResourceName(),
                entity.getProvince(),
                entity.getCity(),
                entity.getVehicleType(),
                entity.getSeatCount(),
                entity.getBillingMode(),
                entity.getProcurementMode()
        );
    }
}
