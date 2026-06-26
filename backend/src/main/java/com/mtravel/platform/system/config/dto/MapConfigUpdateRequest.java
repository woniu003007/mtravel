package com.mtravel.platform.system.config.dto;

import jakarta.validation.constraints.Size;

/**
 * 高德地图配置保存请求。
 *
 * <p>空 Key 表示不覆盖已保存密钥，仅用于页面回显后保存其它配置的场景。</p>
 *
 * @param webServiceKey 高德 Web 服务 Key。
 * @param jsKey 高德 JS API Key。
 * @param jsSecurityCode 高德 JS API 安全密钥。
 */
public record MapConfigUpdateRequest(
        @Size(max = 200, message = "高德Web服务Key最多200个字符")
        String webServiceKey,
        @Size(max = 200, message = "高德JS Key最多200个字符")
        String jsKey,
        @Size(max = 200, message = "高德安全密钥最多200个字符")
        String jsSecurityCode
) {
}
