package com.mtravel.platform.dispatch.guide.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveSaveRequest;
import com.mtravel.platform.dispatch.guide.dto.GuideSelfLeaveSaveRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导游端请假接口。
 *
 * <p>第一版导游端仍复用当前管理端应用和登录体系，通过登录账号匹配导游档案 username。</p>
 */
@RestController
@RequestMapping("/guide/my-leave")
public class GuidePortalLeaveController extends ControllerSupport {

    private final DispatchGuideScheduleService service;

    /**
     * 构造导游端请假 Controller。
     */
    public GuidePortalLeaveController(DispatchGuideScheduleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 查询我的请假记录。
     */
    @OperationLog(module = "导游端", type = "查询")
    @GetMapping
    public ApiResponse<List<GuideLeaveResponse>> myLeaves(Authentication authentication) {
        return ApiResponse.ok(service.myLeaves(currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 导游提交请假申请。
     */
    @OperationLog(module = "导游端", type = "新增")
    @PostMapping
    public ApiResponse<GuideLeaveResponse> submit(
            @Valid @RequestBody GuideSelfLeaveSaveRequest request,
            Authentication authentication
    ) {
        GuideLeaveSaveRequest saveRequest = new GuideLeaveSaveRequest(
                null,
                request.startAt(),
                request.endAt(),
                request.leaveReason(),
                request.remark()
        );
        return ApiResponse.ok(service.submitLeaveByGuide(saveRequest, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 撤回待审批请假申请。
     */
    @OperationLog(module = "导游端", type = "修改")
    @PostMapping("/{leaveId}/withdraw")
    public ApiResponse<GuideLeaveResponse> withdraw(@PathVariable Long leaveId, Authentication authentication) {
        return ApiResponse.ok(service.withdrawLeave(leaveId, currentTenantId(), currentOperator(authentication)));
    }
}
