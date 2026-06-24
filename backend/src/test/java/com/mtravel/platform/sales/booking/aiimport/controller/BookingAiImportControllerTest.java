package com.mtravel.platform.sales.booking.aiimport.controller;

import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportRequest;
import com.mtravel.platform.system.log.web.OperationLog;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 确认单 AI 辅助录入接口元数据测试。
 *
 * <p>系统操作日志表对 operation_type 有固定约束，接口注解必须使用允许值，避免业务接口成功但日志落库失败。</p>
 */
class BookingAiImportControllerTest {

    @Test
    void recognizeShouldUseAllowedOperationLogType() throws NoSuchMethodException {
        OperationLog operationLog = BookingAiImportController.class
                .getMethod("recognize", BookingAiImportRequest.class, Authentication.class)
                .getAnnotation(OperationLog.class);

        assertThat(operationLog).isNotNull();
        assertThat(operationLog.type()).isEqualTo("导入");
    }
}
