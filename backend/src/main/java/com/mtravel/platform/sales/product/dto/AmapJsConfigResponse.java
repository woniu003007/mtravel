package com.mtravel.platform.sales.product.dto;

/**
 * 高德 JavaScript 地图前端加载配置。
 *
 * @param key 高德 Web 端 JS API Key
 * @param securityJsCode 高德安全密钥，用于 JS API 2.0 安全校验
 */
public record AmapJsConfigResponse(
        String key,
        String securityJsCode
) {}
