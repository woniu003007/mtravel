package com.mtravel.platform.sales.booking.order.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingFeeChangeCreateRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingFeeChangeResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingGuestImportPreviewResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderManageRowResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderTaggingRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingTeamDraftResponse;
import com.mtravel.platform.sales.booking.order.service.SalesBookingOrderService;
import com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse;
import com.mtravel.platform.sales.team.service.SalesTeamScheduleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     * 查询全局订单管理列表。
     *
     * @return 订单管理分页行
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/orders/page")
    public ApiResponse<PageResult<SalesBookingOrderManageRowResponse>> orderManagePage(
            @RequestParam(required = false) String groupNo,
            @RequestParam(required = false) String customerTeamNo,
            @RequestParam(required = false) String buyerOrSalespersonKeyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String productKeyword,
            @RequestParam(required = false) String trafficOrPickupRemark,
            @RequestParam(required = false) BigDecimal priceAll,
            @RequestParam(required = false) String bookedBy,
            @RequestParam(required = false) String guestKeyword,
            @RequestParam(required = false) String teamType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "booked") String orderByType,
            @RequestParam(required = false) Boolean tagging,
            @RequestParam(required = false) Boolean hasOrderFile,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.ok(orderService.orderManagePage(
                currentTenantId(),
                groupNo,
                customerTeamNo,
                buyerOrSalespersonKeyword,
                startDate,
                endDate,
                productKeyword,
                trafficOrPickupRemark,
                priceAll,
                bookedBy,
                guestKeyword,
                teamType,
                status,
                orderByType,
                tagging,
                hasOrderFile,
                page,
                pageSize
        ));
    }

    /**
     * 更新订单管理页标记状态。
     *
     * @param id 订单 ID
     * @param request 标记请求
     * @return 操作结果
     */
    @OperationLog(module = "销售管理", type = "标记")
    @PostMapping("/orders/{id}/tagging")
    public ApiResponse<Void> updateOrderTagging(
            @PathVariable Long id,
            @Valid @RequestBody SalesBookingOrderTaggingRequest request
    ) {
        orderService.updateOrderTagging(id, request.tagging(), currentTenantId());
        return ApiResponse.ok();
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

    /**
     * 新增订单费用变更并立即生效。
     *
     * @param orderId 订单 ID
     * @param request 费用变更新增请求
     * @param authentication 当前认证信息
     * @return 已登记的费用变更
     */
    @OperationLog(module = "销售管理", type = "新增")
    @PostMapping("/fee-change/create")
    public ApiResponse<SalesBookingFeeChangeResponse> createFeeChange(
            @RequestParam Long orderId,
            @Valid @RequestBody SalesBookingFeeChangeCreateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.createFeeChange(
                orderId,
                request,
                currentTenantId(),
                currentOperator(authentication)
        ));
    }

    /**
     * 作废订单费用变更。
     *
     * @param id 费用变更 ID
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @OperationLog(module = "销售管理", type = "作废")
    @PostMapping("/fee-change/cancel")
    public ApiResponse<Void> cancelFeeChange(@RequestParam Long id, Authentication authentication) {
        orderService.cancelFeeChange(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /**
     * 导出老系统格式游客名单。
     *
     * @param id 订单 ID
     * @param response HTTP 响应
     */
    @OperationLog(module = "销售管理", type = "导出")
    @GetMapping("/guest-export")
    public void exportGuests(@RequestParam Long id, HttpServletResponse response) throws IOException {
        ByteArrayOutputStream workbook = orderService.exportGuestWorkbook(id, currentTenantId());
        String filename = orderService.guestExportFilename(id, currentTenantId());
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(workbook.size());
        workbook.writeTo(response.getOutputStream());
    }

    /**
     * 下载空白游客名单导入模板。
     *
     * @param response HTTP 响应
     */
    @OperationLog(module = "销售管理", type = "导出")
    @GetMapping("/guest-import/template")
    public void downloadGuestImportTemplate(HttpServletResponse response) throws IOException {
        ByteArrayOutputStream workbook = orderService.guestImportTemplateWorkbook();
        String filename = orderService.guestImportTemplateFilename();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(workbook.size());
        workbook.writeTo(response.getOutputStream());
    }

    /**
     * 预览导入游客名单 Excel。
     *
     * @param file 游客名单 Excel 文件，支持 xls/xlsx
     * @return 解析出的游客草稿和校验提示
     */
    @OperationLog(module = "销售管理", type = "导入")
    @PostMapping("/guest-import/preview")
    public ApiResponse<SalesBookingGuestImportPreviewResponse> importGuestsPreview(
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return ApiResponse.ok(orderService.importGuestWorkbookPreview(
                file.getInputStream(),
                file.getOriginalFilename()
        ));
    }
}
