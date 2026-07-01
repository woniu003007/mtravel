package com.mtravel.platform.dispatch.teamarrangement.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveResponse;
import com.mtravel.platform.dispatch.teamarrangement.service.DispatchScenicTicketGuestExportService;
import com.mtravel.platform.dispatch.teamarrangement.service.DispatchTeamArrangementService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 正式团队安排成本接口。
 *
 * <p>接口路径挂在销售团队上下文下，业务实现放在计调团队安排域。保存逻辑只处理当前一条安排
 * 或当前一次多订单均摊拆分，不重建整个团队安排页面。</p>
 */
@RestController
public class DispatchTeamArrangementController extends ControllerSupport {

    private final DispatchTeamArrangementService service;
    private final DispatchScenicTicketGuestExportService scenicTicketGuestExportService;

    /**
     * 构造正式团队安排成本 Controller。
     */
    public DispatchTeamArrangementController(
            DispatchTeamArrangementService service,
            DispatchScenicTicketGuestExportService scenicTicketGuestExportService,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
        this.scenicTicketGuestExportService = scenicTicketGuestExportService;
    }

    /**
     * 查询团队安排成本列表。
     *
     * @param teamId 团队 ID
     * @param type 资源类型，可为空
     * @return 团队安排成本列表
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/sales/team/{teamId}/arrangements")
    public ApiResponse<List<TeamArrangementResponse>> list(
            @PathVariable Long teamId,
            @RequestParam(required = false) String type
    ) {
        return ApiResponse.ok(service.list(teamId, type, currentTenantId()));
    }

    /**
     * 保存团队安排成本。
     *
     * @param teamId 团队 ID
     * @param request 保存请求
     * @param authentication 当前登录信息
     * @return 保存结果
     */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/sales/team/{teamId}/arrangements/save")
    public ApiResponse<TeamArrangementSaveResponse> save(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamArrangementSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.save(teamId, request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 删除团队安排成本。
     *
     * @param teamId 团队 ID
     * @param arrangementId 安排 ID
     * @param authentication 当前登录信息
     * @return 空响应
     */
    @OperationLog(module = "计调操作", type = "删除")
    @PostMapping("/sales/team/{teamId}/arrangements/{arrangementId}/delete")
    public ApiResponse<Void> delete(
            @PathVariable Long teamId,
            @PathVariable Long arrangementId,
            Authentication authentication
    ) {
        service.delete(teamId, arrangementId, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /**
     * 下载景区票务系统游客名单 Excel。
     *
     * @param teamId 团队 ID
     * @param resourceName 景区资源名称
     * @param supplierId 供应商 ID
     * @param response HTTP 响应
     */
    @OperationLog(module = "计调操作", type = "导出")
    @GetMapping("/sales/team/{teamId}/arrangements/scenic-ticket-guests/export")
    public void exportScenicTicketGuests(
            @PathVariable Long teamId,
            @RequestParam String resourceName,
            @RequestParam Long supplierId,
            HttpServletResponse response
    ) throws IOException {
        DispatchScenicTicketGuestExportService.ExportResult result =
                scenicTicketGuestExportService.export(teamId, resourceName, supplierId, currentTenantId());
        ByteArrayOutputStream workbook = result.content();
        String encodedFilename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(workbook.size());
        workbook.writeTo(response.getOutputStream());
    }
}
