package com.mtravel.platform.sales.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 销售产品每日路书地点保存请求。
 *
 * @param pointOrder 当天地点顺序
 * @param placeName 地点名称
 * @param address 地点地址
 * @param longitude 经度
 * @param latitude 纬度
 * @param pointType 地点类型
 * @param stayMinutes 停留分钟数
 * @param distanceToNextMeters 到下一站距离，单位米
 * @param durationToNextSeconds 到下一站预计车程，单位秒
 * @param remark 备注
 */
@Schema(name = "SalesProductRoadbookPointRequest", description = "销售产品每日路书地点保存请求")
public record SalesProductRoadbookPointRequest(
        @Schema(description = "当天地点顺序，从 1 开始", example = "1", minimum = "1")
        @Min(value = 1, message = "路书地点顺序必须从1开始")
        Integer pointOrder,

        @Schema(description = "地点名称", example = "夫子庙秦淮风光带", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "路书地点名称不能为空") @Size(max = 200)
        String placeName,

        @Schema(description = "地点地址", example = "南京市秦淮区贡院街")
        @Size(max = 300)
        String address,

        @Schema(description = "经度", example = "118.7881")
        @Size(max = 40)
        String longitude,

        @Schema(description = "纬度", example = "32.0208")
        @Size(max = 40)
        String latitude,

        @Schema(
                description = "地点类型：departure 出发，waypoint 途经，scenic 景区，meal 用餐，shopping 购物，hotel 酒店，arrival 抵达",
                example = "scenic",
                allowableValues = {"departure", "waypoint", "scenic", "meal", "shopping", "hotel", "arrival"}
        )
        @Pattern(
                regexp = "departure|waypoint|scenic|meal|shopping|hotel|arrival",
                message = "路书地点类型不合法"
        )
        String pointType,

        @Schema(description = "停留分钟数", example = "90", minimum = "0")
        @Min(value = 0, message = "停留时长不能小于0")
        Integer stayMinutes,

        @Schema(description = "到下一站距离，单位米", example = "8000", minimum = "0")
        @Min(value = 0, message = "到下一站距离不能小于0")
        Integer distanceToNextMeters,

        @Schema(description = "到下一站预计车程，单位秒", example = "1200", minimum = "0")
        @Min(value = 0, message = "到下一站车程不能小于0")
        Integer durationToNextSeconds,

        @Schema(description = "备注", example = "可根据交通情况调整停留时间")
        @Size(max = 500)
        String remark
) {}
