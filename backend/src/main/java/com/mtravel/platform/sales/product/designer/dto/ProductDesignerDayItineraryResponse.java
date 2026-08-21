package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;

/** 产品设计工作台当天住宿城市和三餐响应。 */
public record ProductDesignerDayItineraryResponse(
        Integer dayNo,
        String accommodationCity,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded
) {
    public static ProductDesignerDayItineraryResponse fromEntity(SalesProductItineraryDayEntity entity, Integer dayNo) {
        return new ProductDesignerDayItineraryResponse(
                dayNo,
                entity == null ? null : entity.getRelatedHotel(),
                entity != null && Boolean.TRUE.equals(entity.getBreakfastIncluded()),
                entity != null && Boolean.TRUE.equals(entity.getLunchIncluded()),
                entity != null && Boolean.TRUE.equals(entity.getDinnerIncluded())
        );
    }
}
