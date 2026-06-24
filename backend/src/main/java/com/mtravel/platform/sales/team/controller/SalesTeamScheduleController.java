package com.mtravel.platform.sales.team.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchCreateRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchEditRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamPriceResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamPriceSaveRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamSaveRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamStatusChangeRequest;
import com.mtravel.platform.sales.team.service.SalesTeamScheduleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销售产品团期管理接口。
 *
 * <p>Controller 只接收页面参数、解析租户和操作人。团期生成、团号、客户类型价格和状态流转规则
 * 均由 SalesTeamScheduleService 处理。</p>
 */
@Validated
@RestController
@RequestMapping("/sales/team/schedule")
public class SalesTeamScheduleController extends ControllerSupport {

    private final SalesTeamScheduleService service;

    public SalesTeamScheduleController(SalesTeamScheduleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询产品团期。
     *
     * @param productId 产品 ID
     * @param startDate 发团开始日期
     * @param endDate 发团结束日期
     * @param status 团队状态
     * @param keyword 团号或操作计调关键字
     * @param page 当前页
     * @param pageSize 每页数量，最大 200
     * @return 团期分页结果
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<SalesTeamResponse>> page(
            @RequestParam Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(),
                productId,
                startDate,
                endDate,
                status,
                keyword,
                page,
                pageSize
        ));
    }

    /**
     * 批量新增团期。
     *
     * @param productId 产品 ID
     * @param request 批量生成请求
     * @param authentication 当前登录信息
     * @return 新增团队列表
     */
    @OperationLog(module = "销售管理", type = "新增")
    @PostMapping("/batch-create")
    public ApiResponse<List<SalesTeamResponse>> batchCreate(
            @RequestParam Long productId,
            @Valid @RequestBody SalesTeamBatchCreateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.batchCreate(
                productId,
                request,
                currentTenantId(),
                currentOperator(authentication)
        ));
    }

    /**
     * 保存团队主信息。
     *
     * @param teamId 团队 ID
     * @param request 团队保存请求
     * @param authentication 当前登录信息
     * @return 修改后的团队信息
     */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/team/save")
    public ApiResponse<SalesTeamResponse> saveTeam(
            @RequestParam Long teamId,
            @Valid @RequestBody SalesTeamSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.saveTeam(teamId, request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 保存客户类型价格。
     *
     * @param teamId 团队 ID
     * @param request 价格保存请求
     * @param authentication 当前登录信息
     * @return 保存后的价格行
     */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/price/save")
    public ApiResponse<SalesTeamPriceResponse> savePrice(
            @RequestParam Long teamId,
            @Valid @RequestBody SalesTeamPriceSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.savePrice(teamId, request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 批量编辑团期和客户类型价格。
     *
     * @param request 批量编辑请求
     * @param authentication 当前登录信息
     * @return 空响应
     */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/batch-edit")
    public ApiResponse<Void> batchEdit(
            @Valid @RequestBody SalesTeamBatchEditRequest request,
            Authentication authentication
    ) {
        service.batchEdit(request, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /**
     * 删除团队价格行。
     *
     * @param priceId 价格行 ID
     * @param authentication 当前登录信息
     * @return 空响应
     */
    @OperationLog(module = "销售管理", type = "删除")
    @PostMapping("/price/delete")
    public ApiResponse<Void> deletePrice(@RequestParam Long priceId, Authentication authentication) {
        service.deletePrice(priceId, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /**
     * 批量变更团队状态。
     *
     * @param request 状态动作请求
     * @param authentication 当前登录信息
     * @return 空响应
     */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/status/change")
    public ApiResponse<Void> changeStatus(
            @Valid @RequestBody SalesTeamStatusChangeRequest request,
            Authentication authentication
    ) {
        service.changeStatus(
                request.teamIds(),
                request.action(),
                currentTenantId(),
                currentOperator(authentication),
                request.remark()
        );
        return ApiResponse.ok();
    }
}
