package com.mtravel.platform.agent.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Agent 写接口的幂等键校验、规范 JSON 和 SHA-256 哈希工具。 */
public final class AgentIdempotencySupport {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

    private AgentIdempotencySupport() {
    }

    /** 校验 Agent 写接口幂等键。 */
    public static String requireKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw AgentException.validation("幂等键格式不合法", Map.of("X-Idempotency-Key", "invalid"));
        }
        return key;
    }

    /** 对默认值补齐后的请求生成不受 JSON 字段顺序影响的哈希。 */
    public static String hash(ObjectMapper objectMapper, Object value) {
        try {
            JsonNode canonical = sort(objectMapper.valueToTree(value));
            byte[] bytes = objectMapper.writeValueAsBytes(canonical);
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法生成 Agent 请求幂等哈希", exception);
        }
    }

    /** 将 JSON 规范化为键名排序的紧凑字符串。 */
    public static String canonicalJson(ObjectMapper objectMapper, JsonNode value) {
        try {
            return objectMapper.writeValueAsString(sort(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化 Agent 结构化需求", exception);
        }
    }

    private static JsonNode sort(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) return node;
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> array.add(sort(item)));
            return array;
        }
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        iterator.forEachRemaining(fields::add);
        fields.sort(Comparator.comparing(Map.Entry::getKey));
        fields.forEach(field -> object.set(field.getKey(), sort(field.getValue())));
        return object;
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}
