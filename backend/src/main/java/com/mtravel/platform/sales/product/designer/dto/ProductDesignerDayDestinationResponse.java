package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;

/** 产品设计工作台当天主行程城市响应。 */
public record ProductDesignerDayDestinationResponse(
        Integer dayNo,
        String destinationProvince,
        String destinationCity,
        String destinationDistrict
) {
    public static ProductDesignerDayDestinationResponse fromEntity(
            SalesProductItineraryDayEntity entity,
            Integer dayNo
    ) {
        return new ProductDesignerDayDestinationResponse(
                dayNo,
                entity == null ? null : entity.getDestinationProvince(),
                entity == null ? null : entity.getDestinationCity(),
                entity == null ? null : entity.getDestinationDistrict()
        );
    }
}
