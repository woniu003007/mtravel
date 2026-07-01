package com.mtravel.platform.dispatch.guide.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveReviewRequest;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveSaveRequest;
import com.mtravel.platform.dispatch.guide.dto.GuideScheduleCalendarResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideScheduleQuery;
import com.mtravel.platform.dispatch.guide.service.DispatchGuideScheduleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台导游排班和请假管理接口。
 *
 * <p>计调在这里查看排班汇总、审批导游请假和直接设置导游不可上团。</p>
 */
@Validated
@RestController
@RequestMapping("/dispatch")
public class DispatchGuideScheduleController extends ControllerSupport {

    private final DispatchGuideScheduleService service;

    /**
     * 构造后台导游排班 Controller。
     */
    public DispatchGuideScheduleController(DispatchGuideScheduleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 查询导游排班日历。
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/guide-schedule/calendar")
    public ApiResponse<GuideScheduleCalendarResponse> calendar(
            @RequestParam(required = false) String guideName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return ApiResponse.ok(service.calendar(new GuideScheduleQuery(guideName, startDate), currentTenantId()));
    }

    /**
     * 分页查询导游请假审批列表。
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/guide-leaves/page")
    public ApiResponse<PageResult<GuideLeaveResponse>> pageLeaves(
            @RequestParam(required = false) String guideName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.pageLeaves(guideName, status, startDate, endDate, page, pageSize, currentTenantId()));
    }

    /**
     * 计调直接设置导游不可上团。
     */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/guide-leaves/direct")
    public ApiResponse<GuideLeaveResponse> directCreateLeave(
            @Valid @RequestBody GuideLeaveSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.directCreateLeave(request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 审批通过导游请假。
     */
    @OperationLog(module = "计调操作", type = "审批")
    @PostMapping("/guide-leaves/{leaveId}/approve")
    public ApiResponse<GuideLeaveResponse> approve(
            @PathVariable Long leaveId,
            @Valid @RequestBody GuideLeaveReviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.approveLeave(leaveId, request.approvalRemark(), currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 驳回导游请假。
     */
    @OperationLog(module = "计调操作", type = "审批")
    @PostMapping("/guide-leaves/{leaveId}/reject")
    public ApiResponse<GuideLeaveResponse> reject(
            @PathVariable Long leaveId,
            @Valid @RequestBody GuideLeaveReviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.rejectLeave(leaveId, request.approvalRemark(), currentTenantId(), currentOperator(authentication)));
    }
}
