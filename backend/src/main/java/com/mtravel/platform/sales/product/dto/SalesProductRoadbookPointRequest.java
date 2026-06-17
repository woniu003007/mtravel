package com.mtravel.platform.sales.product.dto;

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
public record SalesProductRoadbookPointRequest(
        @Min(value = 1, message = "路书地点顺序必须从1开始") Integer pointOrder,
        @NotBlank(message = "路书地点名称不能为空") @Size(max = 200) String placeName,
        @Size(max = 300) String address,
        @Size(max = 40) String longitude,
        @Size(max = 40) String latitude,
        @Pattern(
                regexp = "departure|waypoint|scenic|meal|shopping|hotel|arrival",
                message = "路书地点类型不合法"
        ) String pointType,
        @Min(value = 0, message = "停留时长不能小于0") Integer stayMinutes,
        @Min(value = 0, message = "到下一站距离不能小于0") Integer distanceToNextMeters,
        @Min(value = 0, message = "到下一站车程不能小于0") Integer durationToNextSeconds,
        @Size(max = 500) String remark
) {}
