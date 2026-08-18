package com.mtravel.platform.sales.team.documentimport.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportApplyRequest;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportApplyResponse;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskCreateRequest;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskResponse;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskUpdateRequest;
import com.mtravel.platform.sales.team.documentimport.service.TeamDocumentImportApplyService;
import com.mtravel.platform.sales.team.documentimport.service.TeamDocumentImportTaskService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 团队 Word 智能代录接口，提供异步识别任务、草稿修正和确认写入。 */
@RestController
@RequestMapping("/sales/team/document-import/tasks")
public class TeamDocumentImportController extends ControllerSupport {
    private final TeamDocumentImportTaskService taskService;
    private final TeamDocumentImportApplyService applyService;

    public TeamDocumentImportController(
            TeamDocumentImportTaskService taskService,
            TeamDocumentImportApplyService applyService,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.taskService = taskService;
        this.applyService = applyService;
    }

    /** 创建 Word 识别任务，接口立即返回任务ID。 */
    @OperationLog(module = "销售管理", type = "导入")
    @PostMapping
    public ApiResponse<TeamDocumentImportTaskResponse> create(
            @Valid @RequestBody TeamDocumentImportTaskCreateRequest request, Authentication authentication
    ) {
        return ApiResponse.ok(taskService.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 查询异步任务进度和可编辑草稿。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/{taskId}")
    public ApiResponse<TeamDocumentImportTaskResponse> detail(@PathVariable Long taskId) {
        return ApiResponse.ok(taskService.detail(taskId, currentTenantId()));
    }

    /** 保存计调修改后的草稿、资源候选和供应商选择。 */
    @OperationLog(module = "销售管理", type = "修改")
    @PutMapping("/{taskId}/draft")
    public ApiResponse<TeamDocumentImportTaskResponse> updateDraft(
            @PathVariable Long taskId, @Valid @RequestBody TeamDocumentImportTaskUpdateRequest request
    ) {
        return ApiResponse.ok(taskService.updateDraft(taskId, request, currentTenantId()));
    }

    /** 重试失败的 Word 识别任务。 */
    @OperationLog(module = "销售管理", type = "导入")
    @PostMapping("/{taskId}/retry")
    public ApiResponse<TeamDocumentImportTaskResponse> retry(@PathVariable Long taskId) {
        return ApiResponse.ok(taskService.retry(taskId, currentTenantId()));
    }

    /** 团队保存成功后，确认生成订单、游客及已匹配的资源安排。 */
    @OperationLog(module = "销售管理", type = "保存")
    @PostMapping("/{taskId}/apply")
    public ApiResponse<TeamDocumentImportApplyResponse> apply(
            @PathVariable Long taskId, @Valid @RequestBody TeamDocumentImportApplyRequest request, Authentication authentication
    ) {
        return ApiResponse.ok(applyService.apply(taskId, request, currentTenantId(), currentOperator(authentication)));
    }
}
