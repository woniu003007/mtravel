package com.mtravel.platform.sales.booking.aiimport.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportRequest;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportResponse;
import com.mtravel.platform.sales.booking.aiimport.service.BookingAiImportService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "销售管理-订单AI导入", description = "订单确认单、游客名单和微信文本的 AI 识别预览接口。")
@RestController
@RequestMapping("/sales/booking/ai-import")
public class BookingAiImportController extends ControllerSupport {

    private static final String RECOGNIZE_REQUEST_EXAMPLE = """
            {
              "sourceType": "text",
              "text": "客户：南京某旅行社 联系人：王经理 13800000000\\n7月20日南京接团，G7001 09:00到南京南站，7月23日返程。\\n成人2人，成人价1000元/人，单房差300元。\\n游客：张三 320102199001011234，李四 320102199202022345。"
            }
            """;

    private final BookingAiImportService service;

    public BookingAiImportController(BookingAiImportService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 识别确认单并返回可编辑草稿。
     */
    @Operation(
            summary = "识别确认单生成订单草稿",
            description = "识别粘贴文本或已上传附件中的客户、交通、价格和游客名单信息，只返回可编辑草稿，不自动保存正式订单。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "确认单 AI 识别请求。可传 text，也可传 attachmentId 或 attachmentIds。",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "recognizeRequest",
                                    summary = "文本确认单识别示例",
                                    value = RECOGNIZE_REQUEST_EXAMPLE
                            )
                    )
            )
    )
    @OperationLog(module = "销售收客", type = "导入")
    @PostMapping("/recognize")
    public ApiResponse<BookingAiImportResponse> recognize(
            @Valid @RequestBody BookingAiImportRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.recognize(request, currentTenantId(), currentOperator(authentication)));
    }
}
