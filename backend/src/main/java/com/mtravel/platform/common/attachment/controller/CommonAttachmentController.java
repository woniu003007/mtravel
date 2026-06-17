package com.mtravel.platform.common.attachment.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.attachment.dto.AttachmentResponse;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公共附件接口。
 *
 * <p>业务页面统一通过本接口上传合同、确认单等文件。Controller 只解析当前租户和操作人，
 * 文件落盘、路径清洗和元数据保存由 Service 负责。</p>
 */
@Validated
@RestController
@RequestMapping("/common/attachment")
public class CommonAttachmentController extends ControllerSupport {

    private final CommonAttachmentService service;

    public CommonAttachmentController(CommonAttachmentService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 上传附件。 */
    @OperationLog(module = "公共附件", type = "新增")
    @PostMapping("/upload")
    public ApiResponse<AttachmentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String businessModule,
            @RequestParam String businessType,
            @RequestParam(required = false) Long businessId,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.upload(
                file,
                businessModule,
                businessType,
                businessId,
                currentTenantId(),
                currentOperator(authentication)
        ));
    }

    /** 查询某个业务记录下的附件列表。 */
    @OperationLog(module = "公共附件", type = "查询")
    @GetMapping("/list")
    public ApiResponse<List<AttachmentResponse>> list(
            @RequestParam String businessModule,
            @RequestParam String businessType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) long pageSize
    ) {
        // 当前列表按业务记录查询，数量通常很小；page/pageSize 保留给前端统一参数，不参与首版切片。
        return ApiResponse.ok(service.listByBusiness(currentTenantId(), businessModule, businessType, businessId));
    }
}
