package com.mtravel.platform.common.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里云全文识别高精版客户端。
 *
 * <p>使用阿里云 OpenAPI V3 自签名请求，避免在项目中引入额外 SDK 依赖。当前只处理图片，
 * 因为 RecognizeAdvanced 官方接口不接收 PDF；扫描 PDF 由现有视觉识别兜底。</p>
 */
@Component
public class AliyunOcrClient {

    private static final String ALGORITHM = "ACS3-HMAC-SHA256";
    private static final String ACTION = "RecognizeAdvanced";
    private static final String VERSION = "2021-07-07";
    private static final String DEFAULT_ENDPOINT = "ocr-api.cn-hangzhou.aliyuncs.com";

    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endpoint;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AliyunOcrClient(
            @Value("${mtravel.aliyun.ocr.access-key-id:${ALIBABA_CLOUD_ACCESS_KEY_ID:}}") String accessKeyId,
            @Value("${mtravel.aliyun.ocr.access-key-secret:${ALIBABA_CLOUD_ACCESS_KEY_SECRET:}}") String accessKeySecret,
            @Value("${mtravel.aliyun.ocr.endpoint:${ALIYUN_OCR_ENDPOINT:ocr-api.cn-hangzhou.aliyuncs.com}}") String endpoint
    ) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.endpoint = StringUtils.hasText(endpoint) ? endpoint.trim() : DEFAULT_ENDPOINT;
    }

    /** 对图片执行全文识别；未配置密钥或接口失败时返回空，让上层继续兜底。 */
    public Optional<String> recognize(Long tenantId, String sourceType, byte[] content) {
        if (!isImage(sourceType) || content == null || content.length == 0
                || content.length > 10 * 1024 * 1024
                || !StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(accessKeySecret)) {
            return Optional.empty();
        }
        try {
            byte[] body = content;
            String date = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String payloadHash = sha256Hex(body);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("host", endpoint);
            headers.put("x-acs-action", ACTION);
            headers.put("x-acs-content-sha256", payloadHash);
            headers.put("x-acs-date", date);
            headers.put("x-acs-signature-nonce", nonce);
            headers.put("x-acs-version", VERSION);

            String signedHeaders = String.join(";", headers.keySet());
            StringBuilder canonicalHeaders = new StringBuilder();
            headers.forEach((key, value) ->
                    canonicalHeaders.append(key).append(':').append(value.trim()).append('\n'));
            String canonicalRequest = String.join(
                    "\n",
                    "POST",
                    "/",
                    "",
                    canonicalHeaders.toString(),
                    signedHeaders,
                    payloadHash
            );
            String stringToSign = ALGORITHM + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            String signature = hmacHex(accessKeySecret, stringToSign);
            String authorization = "%s Credential=%s,SignedHeaders=%s,Signature=%s"
                    .formatted(ALGORITHM, accessKeyId.trim(), signedHeaders, signature);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create("https://" + endpoint + "/"))
                    .header("x-acs-action", ACTION)
                    .header("x-acs-content-sha256", payloadHash)
                    .header("x-acs-date", date)
                    .header("x-acs-signature-nonce", nonce)
                    .header("x-acs-version", VERSION)
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/octet-stream");
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() / 100 != 2) {
                return Optional.empty();
            }
            return parseContent(response.body());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<String> parseContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.has("Data") ? root.get("Data") : root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return Optional.empty();
            }
            if (dataNode.isTextual()) {
                String data = dataNode.asText("");
                if (!StringUtils.hasText(data)) {
                    return Optional.empty();
                }
                dataNode = objectMapper.readTree(data);
            }
            String content = firstText(dataNode, "content", "Content", "text", "Text");
            if (StringUtils.hasText(content)) {
                return Optional.of(content.trim());
            }
            JsonNode words = dataNode.has("prism_wordsInfo")
                    ? dataNode.get("prism_wordsInfo")
                    : dataNode.get("wordsInfo");
            if (words != null && words.isArray()) {
                StringBuilder text = new StringBuilder();
                words.forEach(word -> {
                    String value = firstText(word, "word", "Word", "text", "Text");
                    if (StringUtils.hasText(value)) {
                        if (text.length() > 0) {
                            text.append('\n');
                        }
                        text.append(value.trim());
                    }
                });
                return StringUtils.hasText(text) ? Optional.of(text.toString()) : Optional.empty();
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }

    private boolean isImage(String sourceType) {
        String type = sourceType == null ? "" : sourceType.trim().toLowerCase();
        return switch (type) {
            case "jpg", "jpeg", "png", "webp", "bmp", "gif", "tiff", "image" -> true;
            default -> false;
        };
    }

    private String sha256Hex(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private String hmacHex(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
