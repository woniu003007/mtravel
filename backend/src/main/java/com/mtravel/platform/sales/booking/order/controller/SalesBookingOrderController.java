package com.mtravel.platform.sales.booking.order.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingTeamDraftResponse;
import com.mtravel.platform.sales.booking.order.service.SalesBookingOrderService;
import com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse;
import com.mtravel.platform.sales.team.service.SalesTeamScheduleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import java.util.List;
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
 * 销售收客订单接口。
 *
 * <p>该接口对应旧系统 BookingAgent 收客页。Controller 只负责参数接收、租户解析和响应包装，
 * 保存规则、游客校验、团队人数联动全部在 Service 层完成。</p>
 */
@Validated
@RestController
@RequestMapping("/sales/booking")
public class SalesBookingOrderController extends ControllerSupport {

    private final SalesBookingOrderService orderService;
    private final SalesTeamScheduleService teamService;

    /**
     * 构造收客订单 Controller。
     *
     * @param orderService 收客订单业务服务
     * @param teamService 团队操作详情服务，用于新增页带出团队草稿
     * @param tenantProperties 租户配置
     */
    public SalesBookingOrderController(
            SalesBookingOrderService orderService,
            SalesTeamScheduleService teamService,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.orderService = orderService;
        this.teamService = teamService;
    }

    /**
     * 查询新增收客订单所需团队草稿。
     *
     * @param teamId 团队 ID
     * @return 团队、产品和说明摘要
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/team/{teamId}")
    public ApiResponse<SalesBookingTeamDraftResponse> teamDraft(@PathVariable Long teamId) {
        SalesTeamOperationResponse detail = teamService.operationDetail(teamId, currentTenantId());
        return ApiResponse.ok(new SalesBookingTeamDraftResponse(detail.team(), detail.product(), detail.content()));
    }

    /**
     * 查询团队下订单列表。
     *
     * @param teamId 团队 ID
     * @return 团队订单行
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/team/{teamId}/orders")
    public ApiResponse<List<SalesTeamOperationResponse.OrderRow>> teamOrders(@PathVariable Long teamId) {
        return ApiResponse.ok(orderService.toOperationRows(
                orderService.listOrdersByTeam(teamId, currentTenantId())
        ));
    }

    /**
     * 查询订单详情。
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<SalesBookingOrderResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(orderService.detail(id, currentTenantId()));
    }

    /**
     * 新增或修改收客订单。
     *
     * @param request 保存请求
     * @param authentication 当前认证信息
     * @return 保存后的订单详情
     */
    @OperationLog(module = "销售管理", type = "保存")
    @PostMapping("/save")
    public ApiResponse<SalesBookingOrderResponse> save(
            @Valid @RequestBody SalesBookingOrderSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.save(request, currentTenantId(), currentOperator(authentication)));
    }
}
