package com.mtravel.platform.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理器测试。
 *
 * <p>参数错误必须返回可理解的业务响应，避免前端把普通缺参显示成服务器内部错误。</p>
 */
class GlobalExceptionHandlerTest {

    @Test
    void handleValidationShouldTreatMissingRequestParameterAsBadRequest() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("resourceName", "String");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("参数校验失败");
    }
}
