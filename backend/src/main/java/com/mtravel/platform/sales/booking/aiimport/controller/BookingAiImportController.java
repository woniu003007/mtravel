package com.mtravel.platform.sales.booking.aiimport.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportRequest;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportResponse;
import com.mtravel.platform.sales.booking.aiimport.service.BookingAiImportService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 确认单 AI 辅助录入接口。
 *
 * <p>Controller 只负责接收请求和解析上下文。识别结果是草稿，不代表订单已保存。</p>
 */
@RestController
@RequestMapping("/sales/booking/ai-import")
public class BookingAiImportController extends ControllerSupport {

    private final BookingAiImportService service;

    public BookingAiImportController(BookingAiImportService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 识别确认单并返回可编辑草稿。
     */
    @OperationLog(module = "销售收客", type = "导入")
    @PostMapping("/recognize")
    public ApiResponse<BookingAiImportResponse> recognize(
            @Valid @RequestBody BookingAiImportRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.recognize(request, currentTenantId(), currentOperator(authentication)));
    }
}
