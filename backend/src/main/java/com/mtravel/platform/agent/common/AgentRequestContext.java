package com.mtravel.platform.agent.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/** Agent 请求 ID 上下文工具。 */
public final class AgentRequestContext {

    public static final String ATTRIBUTE_REQUEST_ID = AgentRequestContext.class.getName() + ".requestId";

    private AgentRequestContext() {
    }

    /** 读取过滤器写入的请求 ID；缺失时生成安全兜底值。 */
    public static String requestId(HttpServletRequest request) {
        Object value = request == null ? null : request.getAttribute(ATTRIBUTE_REQUEST_ID);
        return value instanceof String text && !text.isBlank() ? text : newRequestId();
    }

    /** 生成不包含业务信息的请求 ID。 */
    public static String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
