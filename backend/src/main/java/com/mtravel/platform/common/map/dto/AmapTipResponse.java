package com.mtravel.platform.common.map.dto;

/**
 * 公共高德地点候选返回对象。
 *
 * @param name 地点名称
 * @param address 地点地址
 * @param district 行政区
 * @param longitude 经度
 * @param latitude 纬度
 */
public record AmapTipResponse(
        String name,
        String address,
        String district,
        String longitude,
        String latitude
) {
}
