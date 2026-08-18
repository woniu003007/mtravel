package com.mtravel.platform.common.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.system.config.service.AiConfigService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 知识库向量客户端。
 *
 * <p>当前使用阿里云百炼 OpenAI 兼容 embeddings 接口。未配置 API Key 时不抛错，调用方会把文档
 * 保持为待向量化，避免资源资料上传被 AI 配置阻塞。</p>
 */
@Component
public class KnowledgeEmbeddingClient {

    private static final String EMBEDDING_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
    private static final int EMBEDDING_DIMENSIONS = 1024;

    private final String apiKey;
    private final String embeddingModel;
    private final AiConfigService aiConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeEmbeddingClient(
            @Value("${mtravel.ai.bailian.api-key:${BAILIAN_API_KEY:}}") String apiKey,
            @Value("${mtravel.ai.bailian.embedding-model:${BAILIAN_EMBEDDING_MODEL:text-embedding-v4}}") String embeddingModel,
            AiConfigService aiConfigService,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.apiKey = apiKey;
        this.embeddingModel = embeddingModel;
        this.aiConfigService = aiConfigService;
        this.restTemplate = restTemplateBuilder.build();
    }

    /** 调用 embedding 服务并返回 pgvector 字面量。 */
    public Optional<String> embed(Long tenantId, String text) {
        String resolvedKey = resolveApiKey(tenantId);
        if (!StringUtils.hasText(resolvedKey) || !StringUtils.hasText(text)) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resolvedKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String response = restTemplate.postForObject(
                    EMBEDDING_URL,
                    new HttpEntity<>(
                            java.util.Map.of(
                                    "model", modelName(),
                                    "input", List.of(text),
                                    "dimensions", EMBEDDING_DIMENSIONS
                            ),
                            headers
                    ),
                    String.class
            );
            return parseVector(response);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** 当前向量模型名称。 */
    public String modelName() {
        return StringUtils.hasText(embeddingModel) ? embeddingModel.trim() : "text-embedding-v4";
    }

    private String resolveApiKey(Long tenantId) {
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }
        return aiConfigService.rawValue(tenantId, AiConfigService.BAILIAN_API_KEY);
    }

    private Optional<String> parseVector(String response) {
        if (!StringUtils.hasText(response)) {
            return Optional.empty();
        }
        try {
            JsonNode embedding = objectMapper.readTree(response)
                    .path("data")
                    .path(0)
                    .path("embedding");
            if (!embedding.isArray() || embedding.size() != EMBEDDING_DIMENSIONS) {
                return Optional.empty();
            }
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < embedding.size(); i += 1) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(embedding.path(i).asDouble());
            }
            return Optional.of(builder.append(']').toString());
        } catch (java.io.IOException ex) {
            return Optional.empty();
        }
    }
}
