package com.mtravel.platform.sales.booking.aiimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.system.config.service.AiConfigService;
import java.time.Duration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云百炼模型客户端。
 *
 * <p>当前类集中管理百炼配置和调用边界。文本识别未配置 Key 时返回空，让本地规则解析器继续工作；
 * 图片/PDF 只能依赖视觉/OCR，缺少 Key 或外部接口异常时必须抛出清晰业务提示。</p>
 */
@Component
public class BailianAiModelClient implements AiModelClient {

    private static final Logger log = LoggerFactory.getLogger(BailianAiModelClient.class);
    private static final String BAILIAN_COMPATIBLE_CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String DEFAULT_OCR_PROMPT = "请准确提取确认单、游客名单、分房、领队、航班、价格和附加说明。"
            + "保持原文姓名、身份证号、手机号，不要丢失游客行。只返回可读文本。";
    private static final String DOCUMENT_IMPORT_PROMPT = """
            你是旅游地接业务确认单的结构化识别助手。根据提供的文本识别团队录入候选，严格只返回 JSON，
            不要 Markdown，不要解释，不要猜测没有证据的字段。输入文本前面可能包含【当前企业】和【原始文件名】元数据，
            必须用它们帮助判断“本方 / 外部委托方”和团队名称。团队名称禁止返回“华东地接确认件”“确认单”“接待协议”这类通用标题；
            如果正文标题是通用标题，应优先结合文件名语义提炼产品/团队名。凡是每日行程中明确安排的景区、酒店、餐厅、车辆、地接社或购物店，
            都必须放入顶层 resources 数组；不要因为资源名称出现在一句行程描述中就省略。JSON 格式：
            {
              "documentType":"ground_confirmation|product_itinerary|quotation|guest_list|mixed",
              "teamName":null,
              "teamNameEvidence":null,
              "teamNameConfidence":0,
              "departureDate":"YYYY-MM-DD|null","travelDays":null,
              "parties":[
                {
                  "direction":"sender|receiver|other",
                  "businessRole":"customer|entrusting_party|our_company|receiving_party|ground_agent|unknown",
                  "companyName":null,
                  "contactName":null,
                  "phoneToken":null,
                  "evidence":null,
                  "confidence":0
                }
              ],
              "resources":[{"dayNo":1,"time":"HH:mm|null","resourceType":"scenic|hotel|meal|vehicle|ground_agent|shopping|other","resourceName":"","city":null,"remark":null}],
              "itineraryDays":[{"dayNo":1,"dayTitle":null,"content":null,"accommodation":null,"breakfast":false,"lunch":false,"dinner":false}],
              "productDescription":{
                "content":null,"feeIncluded":null,"feeExcluded":null,"childPolicy":null,
                "shoppingArrangement":null,"optionalItems":null,"giftItems":null,
                "attentionItems":null,"warmReminder":null
              },
              "warnings":[],
              "evidence":[]
            }
            resourceType 必须使用上面的英文枚举，resourceName 必须填写名称，city 能识别时必须填写。time 仅在原文明确出现该资源的安排时刻时填写 HH:mm，
            没有可靠时刻时必须填 null，不能根据行程顺序猜测。文本中的身份证号和手机号可能已被占位符替换，必须原样忽略，不能补造。FROM、TO、ATTN、甲乙方、客户单位和联系人
            中出现的公司只放入 parties，绝不能放入 resources，即使名称像旅行社或地接社。参考酒店可作为计调录入辅助：同一天最多返回一家，remark 填“参考酒店”；若同日有明确指定酒店，则不返回参考酒店。
            远观、路过、自费项目，以及航班、火车、城市间转移等大交通只能保留在备注或 warnings，不作为资源安排。船游、游船、夜游、观光船、画舫等属于 scenic 游览项目，必须作为 scenic 返回。
            必须识别文档中的产品说明区并写入 productDescription：费用包含/包含费用 -> feeIncluded；费用不含/报价不含 -> feeExcluded；儿童安排/儿童说明 -> childPolicy；
            购物安排/购物项目 -> shoppingArrangement；自费项目/自费说明 -> optionalItems；赠送项目 -> giftItems；特别说明/注意事项 -> attentionItems；
            温馨提示、温馨提醒 -> warmReminder。Word 表格可能把标题拆成相邻两行或单元格，
            例如“温馨 / 提示”“特别 / 说明”，仍必须按合并后的标题归类。不要因标题字面不完全相同而遗漏；保留原文条目，
            不要把说明内容误识别成资源。正文中的整体产品介绍可写入 content；没有明确内容时填 null，不能编造。
            itineraryDays.content 只保留当日行程摘要，每天不超过 80 个字，
            不要复制景点介绍、接待标准和游客名单；必须同时完整返回 productDescription、parties 和 resources。手机号占位符可能是 [PHONE_1]、[PHONE_2] 这种格式，必须原样返回到 phoneToken 字段，不能改写。\n\n待识别文本：\n""";

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
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(8))
                // 多页确认单的结构化输出可能超过 90 秒；任务本身已异步，这里留足模型返回时间。
                .setReadTimeout(Duration.ofSeconds(180))
                .build();
    }

    /** 测试构造器，避免单元测试依赖 Spring 配置。 */
    BailianAiModelClient(String apiKey, String textModel, String visionModel) {
        this(apiKey, textModel, visionModel, null, new RestTemplateBuilder());
    }

    /**
     * 调用百炼识别确认单内容。
     *
     * <p>无 Key 时返回空，让本地规则解析器继续工作；有 Key 时通过兼容 Chat 接口请求严格 JSON。
     * 调用方仍必须使用本地校验、资源匹配和人工确认，不能把模型文本直接落库。</p>
     */
    @Override
    public Optional<String> recognize(Long tenantId, String sourceText) {
        String resolvedKey = resolveApiKey(tenantId);
        if (!StringUtils.hasText(resolvedKey)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(sourceText)) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resolvedKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> request = Map.of(
                    "model", resolveTextModel(tenantId),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", DOCUMENT_IMPORT_PROMPT + sourceText
                    )),
                    "temperature", 0,
                    "max_tokens", 8192,
                    "response_format", Map.of("type", "json_object")
            );
            return parseContent(restTemplate.postForObject(
                    BAILIAN_COMPATIBLE_CHAT_URL,
                    new HttpEntity<>(request, headers),
                    String.class
            ));
        } catch (RestClientException ex) {
            // 不记录请求文本、游客信息或密钥，只记录根异常类型用于区分连接失败和读取超时。
            log.warn("bailian text recognition failed: exceptionType={}, rootCauseType={}",
                    ex.getClass().getSimpleName(), rootCauseType(ex));
            return Optional.empty();
        }
    }

    private String rootCauseType(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName();
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
        if (!StringUtils.hasText(resolvedKey)) {
            throw new BizException("当前未配置百炼 API Key，请到系统配置保存百炼配置后再识别图片/PDF");
        }
        if (content == null || content.length == 0) {
            throw new BizException("确认单图片/PDF内容为空，请重新上传文件");
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
            Optional<String> parsedContent = parseContent(response);
            if (parsedContent.isEmpty()) {
                throw new BizException("百炼视觉/OCR识别未返回内容，请检查API Key、模型名称或稍后重试");
            }
            return parsedContent;
        } catch (BizException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BizException("百炼视觉/OCR识别调用失败，请检查API Key、模型名称或稍后重试");
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

    private String resolveTextModel(Long tenantId) {
        String configured = aiConfigService == null ? "" : aiConfigService.rawValue(tenantId, AiConfigService.BAILIAN_TEXT_MODEL);
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        return StringUtils.hasText(textModel) ? textModel.trim() : "qwen-plus";
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
