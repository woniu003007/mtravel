package com.mtravel.platform.system.config.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.system.config.dto.AiConfigResponse;
import com.mtravel.platform.system.config.dto.AiConfigUpdateRequest;
import com.mtravel.platform.system.config.dto.AuthConfigResponse;
import com.mtravel.platform.system.config.dto.AuthConfigUpdateRequest;
import com.mtravel.platform.system.config.service.AiConfigService;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantContextHolder;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/config")
public class SystemConfigController {

    private final AuthConfigService authConfigService;
    private final AiConfigService aiConfigService;
    private final TenantProperties tenantProperties;

    public SystemConfigController(
            AuthConfigService authConfigService,
            AiConfigService aiConfigService,
            TenantProperties tenantProperties
    ) {
        this.authConfigService = authConfigService;
        this.aiConfigService = aiConfigService;
        this.tenantProperties = tenantProperties;
    }

    @OperationLog(module = "系统设置", type = "查询")
    @GetMapping("/auth")
    public ApiResponse<AuthConfigResponse> authConfig() {
        return ApiResponse.ok(authConfigService.getAuthConfig(currentTenantId()));
    }

    @OperationLog(module = "系统设置", type = "修改")
    @PostMapping("/auth/update")
    public ApiResponse<AuthConfigResponse> updateAuthConfig(@Valid @RequestBody AuthConfigUpdateRequest request) {
        return ApiResponse.ok(authConfigService.updateAuthConfig(currentTenantId(), request));
    }

    /** 查询 AI 辅助录入模型配置，API Key 只返回脱敏内容。 */
    @OperationLog(module = "系统设置", type = "查询")
    @GetMapping("/ai")
    public ApiResponse<AiConfigResponse> aiConfig() {
        return ApiResponse.ok(aiConfigService.getAiConfig(currentTenantId()));
    }

    /** 保存 AI 辅助录入模型配置。 */
    @OperationLog(module = "系统设置", type = "修改")
    @PostMapping("/ai/update")
    public ApiResponse<AiConfigResponse> updateAiConfig(@Valid @RequestBody AiConfigUpdateRequest request) {
        return ApiResponse.ok(aiConfigService.updateAiConfig(currentTenantId(), request));
    }

    private Long currentTenantId() {
        return TenantContextHolder.getTenantId(tenantProperties.getDefaultTenantId());
    }
}
