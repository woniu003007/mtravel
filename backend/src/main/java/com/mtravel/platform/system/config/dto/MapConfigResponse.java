package com.mtravel.platform.system.config.dto;

/**
 * 高德地图配置返回对象。
 *
 * <p>接口只返回脱敏 Key；真实 Key 只允许后端服务内部读取。</p>
 *
 * @param webServiceKeyMasked Web 服务 Key 脱敏值。
 * @param jsKeyMasked 浏览器 JS API Key 脱敏值。
 * @param jsSecurityCodeMasked JS API 安全密钥脱敏值。
 */
public record MapConfigResponse(
        String webServiceKeyMasked,
        String jsKeyMasked,
        String jsSecurityCodeMasked
) {
}
