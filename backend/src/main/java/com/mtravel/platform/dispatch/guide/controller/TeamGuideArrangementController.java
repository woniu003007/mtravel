package com.mtravel.platform.dispatch.guide.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideFieldUpdateRequest;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideResponse;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideSaveRequest;
import com.mtravel.platform.dispatch.guide.service.DispatchGuideScheduleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队导游安排接口。
 *
 * <p>对应团队安排页“导游”分区，提供导游行列表、新增、单字段保存和删除能力。
 * 冲突判断、租户边界和导游快照由 DispatchGuideScheduleService 统一处理。</p>
 */
@RestController
public class TeamGuideArrangementController extends ControllerSupport {

    private final DispatchGuideScheduleService service;

    /**
     * 构造团队导游安排 Controller。
     */
    public TeamGuideArrangementController(DispatchGuideScheduleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 查询团队导游安排列表。
     *
     * @param teamId 团队 ID
     * @return 导游安排列表
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/sales/team/{teamId}/guides")
    public ApiResponse<List<TeamGuideResponse>> list(@PathVariable Long teamId) {
        return ApiResponse.ok(service.listTeamGuides(teamId, currentTenantId()));
    }

    /**
     * 新增团队导游安排。
     */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/sales/team/{teamId}/guides/create")
    public ApiResponse<TeamGuideResponse> create(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamGuideSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createTeamGuide(teamId, request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 单字段保存团队导游安排。
     */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/sales/team/{teamId}/guides/{recordId}/field")
    public ApiResponse<TeamGuideResponse> updateField(
            @PathVariable Long teamId,
            @PathVariable Long recordId,
            @Valid @RequestBody TeamGuideFieldUpdateRequest request
    ) {
        return ApiResponse.ok(service.updateTeamGuideField(teamId, recordId, request, currentTenantId()));
    }

    /**
     * 删除团队导游安排。
     */
    @OperationLog(module = "计调操作", type = "删除")
    @PostMapping("/sales/team/{teamId}/guides/{recordId}/delete")
    public ApiResponse<Void> delete(
            @PathVariable Long teamId,
            @PathVariable Long recordId,
            Authentication authentication
    ) {
        service.deleteTeamGuide(teamId, recordId, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
