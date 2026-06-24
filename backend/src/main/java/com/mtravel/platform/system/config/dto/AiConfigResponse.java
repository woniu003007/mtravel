package com.mtravel.platform.system.config.dto;

/**
 * AI 服务配置返回对象。
 *
 * <p>接口只返回脱敏后的 API Key，避免页面截图、浏览器日志或接口日志泄露完整密钥。</p>
 */
public record AiConfigResponse(
        String provider,
        String apiKeyMasked,
        String textModel,
        String visionModel
) {
}
