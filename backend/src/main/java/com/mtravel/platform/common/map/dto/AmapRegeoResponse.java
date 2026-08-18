package com.mtravel.platform.common.map.dto;

/**
 * 高德逆地理编码返回对象。
 *
 * @param address 经纬度对应的格式化详细地址
 */
public record AmapRegeoResponse(String address) {
}
