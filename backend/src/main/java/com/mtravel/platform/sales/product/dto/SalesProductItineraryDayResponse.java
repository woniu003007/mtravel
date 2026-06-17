package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售产品每日行程返回对象。
 *
 * <p>用于产品详情页回显行程 tab。</p>
 */
public record SalesProductItineraryDayResponse(
        Long id,
        Integer dayNo,
        String dayTitle,
        String itineraryContent,
        String accommodationNote,
        String relatedHotel,
        BigDecimal seasonalSurcharge,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded,
        String roadbookPlace,
        String roadbookSummary,
        Integer roadbookTotalDistanceMeters,
        Integer roadbookTotalDurationSeconds,
        List<SalesProductRoadbookPointResponse> roadbookPoints,
        String remark
) {
    /** 将每日行程实体转换为接口响应。 */
    public static SalesProductItineraryDayResponse fromEntity(SalesProductItineraryDayEntity entity) {
        return fromEntity(entity, List.of());
    }

    /** 将每日行程实体和路书地点明细转换为接口响应。 */
    public static SalesProductItineraryDayResponse fromEntity(
            SalesProductItineraryDayEntity entity,
            List<SalesProductRoadbookPointResponse> roadbookPoints
    ) {
        return new SalesProductItineraryDayResponse(
                entity.getId(),
                entity.getDayNo(),
                entity.getDayTitle(),
                entity.getItineraryContent(),
                entity.getAccommodationNote(),
                entity.getRelatedHotel(),
                entity.getSeasonalSurcharge(),
                entity.getBreakfastIncluded(),
                entity.getLunchIncluded(),
                entity.getDinnerIncluded(),
                entity.getRoadbookPlace(),
                entity.getRoadbookSummary(),
                entity.getRoadbookTotalDistanceMeters(),
                entity.getRoadbookTotalDurationSeconds(),
                roadbookPoints,
                entity.getRemark()
        );
    }
}
