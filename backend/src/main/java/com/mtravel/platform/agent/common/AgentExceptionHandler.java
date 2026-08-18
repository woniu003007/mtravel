package com.mtravel.platform.agent.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Agent 控制器专用异常处理，不改变管理后台全局异常响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.mtravel.platform.agent")
public class AgentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentExceptionHandler.class);

    /** 返回稳定 Agent 业务错误。 */
    @ExceptionHandler(AgentException.class)
    public ResponseEntity<AgentApiResponse<Void>> handleAgent(
            AgentException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.httpStatus()).body(AgentApiResponse.fail(
                exception.code(),
                exception.getMessage(),
                AgentRequestContext.requestId(request),
                new AgentError(exception.errorType(), exception.retryable(), exception.details())
        ));
    }

    /** 返回包含字段信息的参数校验错误。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<AgentApiResponse<Void>> handleValidation(Exception exception, HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException validationException) {
            validationException.getBindingResult().getFieldErrors().forEach(error ->
                    details.putIfAbsent(error.getField(), error.getDefaultMessage())
            );
        } else {
            details.put("request", exception.getMessage());
        }
        AgentException wrapped = AgentException.validation("请求参数校验失败", details);
        return handleAgent(wrapped, request);
    }

    /** 将损坏 JSON、缺失参数和类型转换失败统一为稳定 400 协议。 */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<AgentApiResponse<Void>> handleInvalidArgument(
            Exception exception,
            HttpServletRequest request
    ) {
        AgentException wrapped = AgentException.invalidArgument(
                "请求参数格式不正确",
                Map.of("request", exception.getClass().getSimpleName())
        );
        return handleAgent(wrapped, request);
    }

    /** Agent 未分类异常只记录请求 ID，不向调用方返回堆栈。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AgentApiResponse<Void>> handleUnknown(Exception exception, HttpServletRequest request) {
        String requestId = AgentRequestContext.requestId(request);
        log.error("agent api request failed, requestId={}", requestId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AgentApiResponse.fail(
                50001,
                "服务器内部错误",
                requestId,
                new AgentError("INTERNAL_ERROR", false, Map.of())
        ));
    }
}
