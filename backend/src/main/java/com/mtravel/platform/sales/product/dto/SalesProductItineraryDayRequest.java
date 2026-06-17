package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售产品每日行程保存请求。
 *
 * @param dayNo 行程第几天，从 1 开始
 * @param dayTitle 当日行程标题
 * @param itineraryContent 当日行程内容
 * @param accommodationNote 住宿说明
 * @param relatedHotel 关联酒店名称或说明
 * @param seasonalSurcharge 旺季附加费
 * @param breakfastIncluded 是否含早餐
 * @param lunchIncluded 是否含中餐
 * @param dinnerIncluded 是否含晚餐
 * @param roadbookPlace 路书地点或关键途经地点
 * @param roadbookSummary 当天路书摘要
 * @param roadbookTotalDistanceMeters 当天路书总距离，单位米
 * @param roadbookTotalDurationSeconds 当天路书预计总车程，单位秒
 * @param roadbookPoints 当天路书地点明细
 */
public record SalesProductItineraryDayRequest(
        @Min(value = 1, message = "行程天数必须从1开始") Integer dayNo,
        @Size(max = 200) String dayTitle,
        String itineraryContent,
        @Size(max = 300) String accommodationNote,
        @Size(max = 200) String relatedHotel,
        BigDecimal seasonalSurcharge,
        Boolean breakfastIncluded,
        Boolean lunchIncluded,
        Boolean dinnerIncluded,
        @Size(max = 300) String roadbookPlace,
        @Size(max = 500) String roadbookSummary,
        @Min(value = 0, message = "路书总距离不能小于0") Integer roadbookTotalDistanceMeters,
        @Min(value = 0, message = "路书总车程不能小于0") Integer roadbookTotalDurationSeconds,
        List<SalesProductRoadbookPointRequest> roadbookPoints
) {}
