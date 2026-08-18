package com.mtravel.platform.agent.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/** Agent 聊天原文和结构化文本的纯文本规范化工具。 */
public final class AgentInputSanitizer {

    private static final Pattern HTML_TAG = Pattern.compile("(?is)<\\s*/?\\s*[a-z][^>]*>");

    private AgentInputSanitizer() {
    }

    /** 统一换行并拒绝非法控制字符、HTML 和超长文本。 */
    public static String requiredText(String field, String value, int minLength, int maxLength) {
        if (value == null) throw invalid(field, "required");
        String normalized = normalizeLineEndings(value).trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw invalid(field, "length must be between " + minLength + " and " + maxLength);
        }
        validatePlainText(field, normalized);
        return normalized;
    }

    /** 规范化可选纯文本，空白值统一为 null。 */
    public static String optionalText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        return requiredText(field, value, 1, maxLength);
    }

    /** 递归规范化 JSON 中的所有文本，供幂等哈希和落库共用。 */
    public static JsonNode normalizeJsonStrings(JsonNode node, String fieldPath) {
        if (node == null || node.isNull()) return JsonNodeFactory.instance.nullNode();
        if (node.isTextual()) {
            String value = normalizeLineEndings(node.textValue()).trim();
            if (value.length() > 2000) throw invalid(fieldPath, "length must not exceed 2000");
            validatePlainText(fieldPath, value);
            return JsonNodeFactory.instance.textNode(value);
        }
        if (node.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            for (int index = 0; index < node.size(); index++) {
                result.add(normalizeJsonStrings(node.get(index), fieldPath + "[" + index + "]"));
            }
            return result;
        }
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                result.set(field.getKey(), normalizeJsonStrings(field.getValue(), fieldPath + "." + field.getKey()));
            }
            return result;
        }
        return node.deepCopy();
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void validatePlainText(String field, String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) && character != '\n') {
                throw invalid(field, "contains illegal control characters");
            }
        }
        if (HTML_TAG.matcher(value).find()) throw invalid(field, "HTML is not allowed");
    }

    private static AgentException invalid(String field, String reason) {
        return AgentException.validation("请求文本不符合规则", Map.of(field, reason));
    }
}
