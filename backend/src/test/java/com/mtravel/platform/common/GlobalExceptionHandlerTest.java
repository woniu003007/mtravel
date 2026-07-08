package com.mtravel.platform.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @Test
    void handleMaxUploadSizeShouldReturnReadablePayloadTooLarge() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(50L * 1024 * 1024)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("上传文件不能超过50MB");
    }

    @Test
    void handleNoResourceFoundShouldReturnNotFoundInsteadOfInternalError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/dispatch/room-status/resources")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("接口不存在");
    }
}
