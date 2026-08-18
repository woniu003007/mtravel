package com.mtravel.platform.common.map.dto;

/**
 * 公共高德 JavaScript 地图加载配置。
 *
 * @param key 高德 Web 端 JS API Key
 * @param securityJsCode 高德 JS API 2.0 安全密钥
 */
public record AmapJsConfigResponse(
        String key,
        String securityJsCode
) {
}
