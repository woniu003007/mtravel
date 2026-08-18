package com.mtravel.platform.sales.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "SalesProductItineraryDayRequest", description = "销售产品每日行程保存请求")
public record SalesProductItineraryDayRequest(
        @Schema(description = "行程第几天，从 1 开始", example = "1", minimum = "1")
        @Min(value = 1, message = "行程天数必须从1开始")
        Integer dayNo,

        @Schema(description = "当日行程标题", example = "南京接团-夫子庙")
        @Size(max = 200)
        String dayTitle,

        @Schema(description = "当日行程内容", example = "南京接团后游览夫子庙秦淮风光带，晚餐后入住酒店。")
        String itineraryContent,

        @Schema(description = "住宿说明", example = "南京舒适型酒店")
        @Size(max = 300)
        String accommodationNote,

        @Schema(description = "关联酒店名称或住宿备注", example = "南京市区酒店")
        @Size(max = 200)
        String relatedHotel,

        @Schema(description = "旺季附加费", example = "80.00")
        BigDecimal seasonalSurcharge,

        @Schema(description = "是否含早餐", example = "false")
        Boolean breakfastIncluded,

        @Schema(description = "是否含中餐", example = "true")
        Boolean lunchIncluded,

        @Schema(description = "是否含晚餐", example = "true")
        Boolean dinnerIncluded,

        @Schema(description = "路书地点或关键途经地点摘要", example = "南京站-夫子庙-酒店")
        @Size(max = 300)
        String roadbookPlace,

        @Schema(description = "当天路书摘要", example = "市区短途接送，预计 25 公里。")
        @Size(max = 500)
        String roadbookSummary,

        @Schema(description = "当天路书总距离，单位米", example = "25000", minimum = "0")
        @Min(value = 0, message = "路书总距离不能小于0")
        Integer roadbookTotalDistanceMeters,

        @Schema(description = "当天路书预计总车程，单位秒", example = "3600", minimum = "0")
        @Min(value = 0, message = "路书总车程不能小于0")
        Integer roadbookTotalDurationSeconds,

        @Schema(description = "当天路书地点明细。创建产品基础示例中省略，实际需要地图路书时再传。")
        List<SalesProductRoadbookPointRequest> roadbookPoints
) {}
