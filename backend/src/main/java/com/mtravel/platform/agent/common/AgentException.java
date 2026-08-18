package com.mtravel.platform.agent.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Agent 接口业务异常，携带稳定错误码和可重试标识。 */
public class AgentException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final int code;
    private final String errorType;
    private final boolean retryable;
    private final Map<String, Object> details;

    public AgentException(
            HttpStatus httpStatus,
            int code,
            String errorType,
            String message,
            boolean retryable,
            Map<String, Object> details
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
        this.errorType = errorType;
        this.retryable = retryable;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    /** 未认证或服务令牌无效。 */
    public static AgentException unauthorized(String message) {
        return new AgentException(HttpStatus.UNAUTHORIZED, 40101, "UNAUTHORIZED", message, false, Map.of());
    }

    /** JSON、请求头、路径或查询参数无法正常解析。 */
    public static AgentException invalidArgument(String message, Map<String, Object> details) {
        return new AgentException(
                HttpStatus.BAD_REQUEST,
                40001,
                "INVALID_ARGUMENT",
                message,
                false,
                details
        );
    }

    /** 服务令牌缺少接口 Scope。 */
    public static AgentException forbidden(String message) {
        return new AgentException(HttpStatus.FORBIDDEN, 40301, "FORBIDDEN", message, false, Map.of());
    }

    /** 客户状态或能力限制当前操作。 */
    public static AgentException customerRestricted(String message) {
        return new AgentException(
                HttpStatus.FORBIDDEN,
                40302,
                "CUSTOMER_SERVICE_RESTRICTED",
                message,
                false,
                Map.of()
        );
    }

    /** 客户不存在。 */
    public static AgentException customerNotFound() {
        return new AgentException(HttpStatus.NOT_FOUND, 40401, "CUSTOMER_NOT_FOUND", "未找到客户", false, Map.of());
    }

    /** 资源不存在、越权或关联不一致时使用同一错误，防止枚举资源。 */
    public static AgentException resourceNotFound() {
        return new AgentException(HttpStatus.NOT_FOUND, 40402, "RESOURCE_NOT_FOUND", "未找到可访问的业务资源", false, Map.of());
    }

    /** 请求参数或业务 Schema 不合法。 */
    public static AgentException validation(String message, Map<String, Object> details) {
        return new AgentException(HttpStatus.UNPROCESSABLE_ENTITY, 42201, "VALIDATION_FAILED", message, false, details);
    }

    /** 同一幂等键提交了不同请求体。 */
    public static AgentException idempotencyConflict() {
        return new AgentException(
                HttpStatus.CONFLICT,
                40901,
                "IDEMPOTENCY_CONFLICT",
                "同一幂等键不能对应不同请求",
                false,
                Map.of()
        );
    }

    /** 依赖数据或原子写入结果暂时不可用。 */
    public static AgentException serviceUnavailable(String message) {
        return new AgentException(
                HttpStatus.SERVICE_UNAVAILABLE,
                50301,
                "SERVICE_UNAVAILABLE",
                message,
                true,
                Map.of()
        );
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public int code() {
        return code;
    }

    public String errorType() {
        return errorType;
    }

    public boolean retryable() {
        return retryable;
    }

    public Map<String, Object> details() {
        return details;
    }
}
