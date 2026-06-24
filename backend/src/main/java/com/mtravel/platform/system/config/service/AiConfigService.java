package com.mtravel.platform.system.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.system.config.dto.AiConfigResponse;
import com.mtravel.platform.system.config.dto.AiConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 服务配置管理。
 *
 * <p>用于保存阿里云百炼模型配置。密钥可以存入 system_configs，读取接口只返回脱敏内容；
 * 真正调用模型时由后端服务读取原值。</p>
 */
@Service
public class AiConfigService {

    public static final String AI_PROVIDER = "ai_provider";
    public static final String BAILIAN_API_KEY = "bailian_api_key";
    public static final String BAILIAN_TEXT_MODEL = "bailian_text_model";
    public static final String BAILIAN_VISION_MODEL = "bailian_vision_model";

    private static final String DEFAULT_PROVIDER = "aliyun_bailian";
    private static final String DEFAULT_TEXT_MODEL = "qwen-plus";
    private static final String DEFAULT_VISION_MODEL = "qwen-vl-ocr-latest";

    private final SystemConfigMapper mapper;

    public AiConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 查询当前租户 AI 配置，API Key 只返回脱敏值。 */
    public AiConfigResponse getAiConfig(Long tenantId) {
        String provider = valueOrDefault(tenantId, AI_PROVIDER, DEFAULT_PROVIDER);
        String apiKey = valueOrDefault(tenantId, BAILIAN_API_KEY, "");
        String textModel = valueOrDefault(tenantId, BAILIAN_TEXT_MODEL, DEFAULT_TEXT_MODEL);
        String visionModel = valueOrDefault(tenantId, BAILIAN_VISION_MODEL, DEFAULT_VISION_MODEL);
        return new AiConfigResponse(provider, mask(apiKey), textModel, visionModel);
    }

    /** 保存 AI 配置。空 API Key 不覆盖已保存密钥。 */
    public AiConfigResponse updateAiConfig(Long tenantId, AiConfigUpdateRequest request) {
        upsert(tenantId, AI_PROVIDER, cleanOrDefault(request.provider(), DEFAULT_PROVIDER), "AI模型服务商");
        if (StringUtils.hasText(request.apiKey())) {
            upsert(tenantId, BAILIAN_API_KEY, request.apiKey().trim(), "阿里云百炼API Key");
        }
        upsert(tenantId, BAILIAN_TEXT_MODEL, cleanOrDefault(request.textModel(), DEFAULT_TEXT_MODEL), "阿里云百炼文本识别模型");
        upsert(tenantId, BAILIAN_VISION_MODEL, cleanOrDefault(request.visionModel(), DEFAULT_VISION_MODEL), "阿里云百炼视觉识别模型");
        return new AiConfigResponse(
                cleanOrDefault(request.provider(), DEFAULT_PROVIDER),
                mask(request.apiKey()),
                cleanOrDefault(request.textModel(), DEFAULT_TEXT_MODEL),
                cleanOrDefault(request.visionModel(), DEFAULT_VISION_MODEL)
        );
    }

    /** 读取原始配置值，仅供后端服务内部调用。 */
    public String rawValue(Long tenantId, String key) {
        return valueOrDefault(tenantId, key, "");
    }

    private String valueOrDefault(Long tenantId, String key, String fallback) {
        SystemConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, key));
        if (entity == null || !StringUtils.hasText(entity.getConfigValue())) {
            return fallback;
        }
        return entity.getConfigValue().trim();
    }

    private void upsert(Long tenantId, String key, String value, String remark) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setTenantId(tenantId);
        entity.setConfigKey(key);
        entity.setConfigValue(value);
        entity.setRemark(remark);
        mapper.upsert(entity);
    }

    private String cleanOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() <= 8) {
            return "********";
        }
        return cleaned.substring(0, 4) + "********" + cleaned.substring(cleaned.length() - 4);
    }
}
