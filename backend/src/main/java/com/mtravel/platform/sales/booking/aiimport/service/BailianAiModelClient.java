package com.mtravel.platform.sales.booking.aiimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.system.config.service.AiConfigService;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 阿里云百炼模型客户端。
 *
 * <p>当前类先集中管理百炼配置和调用边界。为了避免开发环境因未配置 Key 直接失败，未配置时返回空，
 * 由本地规则解析器生成草稿。真实 HTTP 调用后续只需要在本类内补齐，不影响 Controller 和前端协议。</p>
 */
@Component
public class BailianAiModelClient implements AiModelClient {

    private static final String BAILIAN_COMPATIBLE_CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String DEFAULT_OCR_PROMPT = "请准确提取确认单、游客名单、分房、领队、航班、价格和附加说明。"
            + "保持原文姓名、身份证号、手机号，不要丢失游客行。只返回可读文本。";

    private final String apiKey;
    private final String textModel;
    private final String visionModel;
    private final AiConfigService aiConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BailianAiModelClient(
            @Value("${mtravel.ai.bailian.api-key:${BAILIAN_API_KEY:}}") String apiKey,
            @Value("${mtravel.ai.bailian.text-model:${BAILIAN_TEXT_MODEL:qwen-plus}}") String textModel,
            @Value("${mtravel.ai.bailian.vision-model:${BAILIAN_VISION_MODEL:qwen-vl-ocr-latest}}") String visionModel,
            AiConfigService aiConfigService,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.apiKey = apiKey;
        this.textModel = textModel;
        this.visionModel = visionModel;
        this.aiConfigService = aiConfigService;
        this.restTemplate = restTemplateBuilder.build();
    }

    /** 测试构造器，避免单元测试依赖 Spring 配置。 */
    BailianAiModelClient(String apiKey, String textModel, String visionModel) {
        this(apiKey, textModel, visionModel, null, new RestTemplateBuilder());
    }

    /**
     * 调用百炼识别确认单内容。
     *
     * <p>本轮先实现配置边界，防止 Key 被写死。无 Key 时让本地规则解析器工作；有 Key 的真实调用可以
     * 在不改变业务协议的前提下接入。</p>
     */
    @Override
    public Optional<String> recognize(Long tenantId, String sourceText) {
        String resolvedKey = resolveApiKey(tenantId);
        if (!StringUtils.hasText(resolvedKey)) {
            return Optional.empty();
        }
        // 真实模型调用必须只在本类内实现，且日志不能打印 apiKey、身份证号或完整游客名单。
        return Optional.empty();
    }

    /**
     * 调用百炼视觉/OCR模型提取图片或扫描件文本。
     *
     * <p>使用 OpenAI 兼容接口，文件内容以 data URL 传给模型。这里不记录请求体，避免身份证号、
     * 游客名单和 API Key 进入日志。</p>
     */
    @Override
    public Optional<String> recognizeImageOrDocument(Long tenantId, String sourceType, byte[] content) {
        String resolvedKey = resolveApiKey(tenantId);
        if (!StringUtils.hasText(resolvedKey) || content == null || content.length == 0) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resolvedKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> request = Map.of(
                    "model", resolveVisionModel(tenantId),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", DEFAULT_OCR_PROMPT),
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl(sourceType, content)))
                            )
                    )),
                    "temperature", 0
            );
            String response = restTemplate.postForObject(
                    BAILIAN_COMPATIBLE_CHAT_URL,
                    new HttpEntity<>(request, headers),
                    String.class
            );
            return parseContent(response);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public String textModel() {
        return textModel;
    }

    public String visionModel() {
        return visionModel;
    }

    private String resolveApiKey(Long tenantId) {
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }
        return aiConfigService == null ? "" : aiConfigService.rawValue(tenantId, AiConfigService.BAILIAN_API_KEY);
    }

    private String resolveVisionModel(Long tenantId) {
        if (StringUtils.hasText(visionModel)) {
            return visionModel.trim();
        }
        String configured = aiConfigService == null ? "" : aiConfigService.rawValue(tenantId, AiConfigService.BAILIAN_VISION_MODEL);
        return StringUtils.hasText(configured) ? configured.trim() : "qwen-vl-ocr-latest";
    }

    private String dataUrl(String sourceType, byte[] content) {
        String type = StringUtils.hasText(sourceType) ? sourceType.trim().toLowerCase() : "png";
        String mimeType = switch (type) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> "image/png";
        };
        return "data:%s;base64,%s".formatted(mimeType, Base64.getEncoder().encodeToString(content));
    }

    private Optional<String> parseContent(String response) {
        if (!StringUtils.hasText(response)) {
            return Optional.empty();
        }
        try {
            JsonNode content = objectMapper.readTree(response)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
                return Optional.empty();
            }
            return Optional.of(content.asText().trim());
        } catch (java.io.IOException ex) {
            return Optional.empty();
        }
    }
}
