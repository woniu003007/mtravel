package com.mtravel.platform.agent.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Agent 请求体的专用严格 JSON 字段校验。
 *
 * <p>系统现有接口保留 Jackson 原配置；本 Advice 只扫描 Agent DTO，防止模型提交未定义的金额、负责人或内部字段。</p>
 */
@RestControllerAdvice(basePackages = "com.mtravel.platform.agent")
public class AgentStrictJsonRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private static final Set<String> PRODUCT_SEARCH_FIELDS = Set.of(
            "customerId", "keyword", "destinations", "businessTypes", "productThemes",
            "receptionStandards", "travelDays", "departureDate", "party", "onlyAvailable", "page", "pageSize"
    );
    private static final Set<String> QUOTE_FIELDS = Set.of(
            "conversationId", "customerId", "quoteType", "relatedProductId", "relatedScheduleId",
            "sourceMessage", "requirements"
    );
    private static final Set<String> HANDOFF_FIELDS = Set.of(
            "conversationId", "customerId", "reasonCode", "priority", "summary", "sourceMessages", "related"
    );
    private static final Set<String> HANDOFF_MESSAGE_FIELDS = Set.of(
            "messageId", "senderName", "sentAt", "content"
    );
    private static final Set<String> HANDOFF_RELATED_FIELDS = Set.of(
            "productId", "scheduleId", "teamNo", "quoteRequestId"
    );

    private final ObjectMapper objectMapper;

    public AgentStrictJsonRequestBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return targetType == AgentProductApi.SearchRequest.class
                || targetType == AgentQuoteApi.CreateRequest.class
                || targetType == AgentHandoffApi.CreateRequest.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException exception) {
            throw AgentException.invalidArgument("JSON 请求体格式不正确", Map.of("request", "malformed_json"));
        }
        if (targetType == AgentProductApi.SearchRequest.class) validateProductSearch(root);
        if (targetType == AgentQuoteApi.CreateRequest.class) validateFields(root, "request", QUOTE_FIELDS);
        if (targetType == AgentHandoffApi.CreateRequest.class) validateHandoff(root);
        return new ReusableHttpInputMessage(inputMessage.getHeaders(), body);
    }

    private void validateProductSearch(JsonNode root) {
        validateFields(root, "request", PRODUCT_SEARCH_FIELDS);
        validateOptionalObject(root, "travelDays", Set.of("min", "max"));
        validateOptionalObject(root, "departureDate", Set.of("from", "to"));
        validateOptionalObject(root, "party", Set.of("adults", "children", "childrenNoBed", "seniors"));
    }

    private void validateHandoff(JsonNode root) {
        validateFields(root, "request", HANDOFF_FIELDS);
        JsonNode messages = root.get("sourceMessages");
        if (messages != null && messages.isArray()) {
            for (int index = 0; index < messages.size(); index++) {
                validateFields(messages.get(index), "sourceMessages[" + index + "]", HANDOFF_MESSAGE_FIELDS);
            }
        }
        validateOptionalObject(root, "related", HANDOFF_RELATED_FIELDS);
    }

    private void validateOptionalObject(JsonNode parent, String field, Set<String> allowedFields) {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull()) return;
        validateFields(value, field, allowedFields);
    }

    private void validateFields(JsonNode object, String path, Set<String> allowedFields) {
        if (object == null || !object.isObject()) {
            throw AgentException.validation("Agent 请求对象格式不正确", Map.of(path, "must be an object"));
        }
        Set<String> unknown = new HashSet<>();
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowedFields.contains(field)) unknown.add(field);
        }
        if (!unknown.isEmpty()) {
            throw AgentException.validation("Agent 请求不允许额外字段", Map.of(path, "unknown fields: " + unknown));
        }
    }

    private record ReusableHttpInputMessage(HttpHeaders headers, byte[] body) implements HttpInputMessage {
        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
