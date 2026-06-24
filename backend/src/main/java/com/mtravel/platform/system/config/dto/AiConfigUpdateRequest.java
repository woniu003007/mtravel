package com.mtravel.platform.system.config.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * AI 服务配置保存请求。
 *
 * <p>百炼 Key 可存入系统配置，但不能写入代码、SQL 或文档。空 Key 表示不修改原 Key，仅修改模型配置。</p>
 */
public record AiConfigUpdateRequest(
        @Pattern(regexp = "aliyun_bailian", message = "当前仅支持阿里云百炼") String provider,
        @Size(max = 200, message = "API Key 最多200个字符") String apiKey,
        @Size(max = 80, message = "文本模型最多80个字符") String textModel,
        @Size(max = 80, message = "视觉模型最多80个字符") String visionModel
) {
}
