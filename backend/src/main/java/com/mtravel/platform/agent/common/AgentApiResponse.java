package com.mtravel.platform.agent.common;

/**
 * Agent 专用接口响应。
 *
 * <p>该对象独立于管理后台 ApiResponse，避免 Agent 协议增加 requestId 和稳定错误枚举时改变旧接口。</p>
 */
public record AgentApiResponse<T>(
        int code,
        String message,
        String requestId,
        T data,
        AgentError error
) {
    /** 构建 Agent 成功响应。 */
    public static <T> AgentApiResponse<T> ok(T data, String requestId) {
        return new AgentApiResponse<>(0, "ok", requestId, data, null);
    }

    /** 构建 Agent 失败响应。 */
    public static AgentApiResponse<Void> fail(
            int code,
            String message,
            String requestId,
            AgentError error
    ) {
        return new AgentApiResponse<>(code, message, requestId, null, error);
    }
}
