package com.mtravel.platform.sales.team.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.team.dto.SalesTeamListResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse;
import com.mtravel.platform.sales.team.service.SalesTeamScheduleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销售团队管理全局列表接口。
 *
 * <p>该 Controller 对应“销售管理 / 团队管理”页面，只负责接收筛选参数和解析当前租户。
 * 团队查询、租户边界、软删除过滤和产品信息批量补充统一交给 SalesTeamScheduleService。</p>
 */
@Validated
@RestController
@RequestMapping("/sales/team")
public class SalesTeamController extends ControllerSupport {

    private final SalesTeamScheduleService service;

    /**
     * 构造销售团队管理 Controller。
     *
     * @param service 团队管理业务服务
     * @param tenantProperties 租户默认配置，用于未登录或测试请求兜底
     */
    public SalesTeamController(SalesTeamScheduleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询销售团队全局列表。
     *
     * @param teamType 团队类型，散拼、整团、散团、单项等
     * @param keyword 团号、团队名称或备注关键字
     * @param operatorKeyword 操作计调关键字
     * @param departurePlace 出发地关键字
     * @param businessType 业务类型
     * @param startDate 出团开始日期
     * @param endDate 出团结束日期
     * @param travelDays 行程天数
     * @param teamStatus 团队状态或页面日期状态
     * @param page 当前页，从 1 开始
     * @param pageSize 每页数量，最大 200
     * @return 团队管理列表分页结果
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<SalesTeamListResponse>> page(
            @RequestParam(required = false) String teamType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) String departurePlace,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer travelDays,
            @RequestParam(required = false) String teamStatus,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.globalPage(
                currentTenantId(),
                teamType,
                keyword,
                operatorKeyword,
                departurePlace,
                businessType,
                startDate,
                endDate,
                travelDays,
                teamStatus,
                page,
                pageSize
        ));
    }

    /**
     * 查询团队操作页只读详情。
     *
     * <p>该接口用于“团队管理”列表点击团号后的执行总览页。第一版只返回团队、产品说明、
     * 价格和按钮状态，不在详情接口里加载订单、员工、供应商等无关大列表。</p>
     *
     * @param teamId 团队 ID
     * @return 团队操作页详情
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/{teamId}/operation")
    public ApiResponse<SalesTeamOperationResponse> operationDetail(@PathVariable Long teamId) {
        return ApiResponse.ok(service.operationDetail(teamId, currentTenantId()));
    }
}
