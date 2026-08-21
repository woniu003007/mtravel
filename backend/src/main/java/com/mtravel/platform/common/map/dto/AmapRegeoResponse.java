package com.mtravel.platform.common.map.dto;

/**
 * 高德逆地理编码返回对象。
 *
 * @param address 经纬度对应的格式化详细地址
 * @param province 省级行政区名称
 * @param city 地级市名称；直辖市使用省级行政区名称回填
 * @param district 区县名称
 */
public record AmapRegeoResponse(
        String address,
        String province,
        String city,
        String district
) {
}
