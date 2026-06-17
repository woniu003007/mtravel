package com.mtravel.platform.system.log.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.system.log.dto.OperationLogResponse;
import com.mtravel.platform.system.log.service.OperationLogService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantContextHolder;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/operation-log")
public class OperationLogController {

    private final OperationLogService service;
    private final TenantProperties tenantProperties;

    public OperationLogController(OperationLogService service, TenantProperties tenantProperties) {
        this.service = service;
        this.tenantProperties = tenantProperties;
    }

    @OperationLog(module = "系统设置", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<OperationLogResponse>> page(
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String requestPath,
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                TenantContextHolder.getTenantId(tenantProperties.getDefaultTenantId()),
                operatorName,
                moduleName,
                requestPath,
                success,
                page,
                pageSize
        ));
    }
}
