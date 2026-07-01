package com.mtravel.platform.sales.team.grossprofit.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.sales.team.grossprofit.dto.SalesTeamGrossProfitPreviewResponse;
import com.mtravel.platform.sales.team.grossprofit.service.SalesTeamGrossProfitService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队预算毛利表接口。
 *
 * <p>该接口支撑团队安排页预算利润点击后的预览、Word 下载、PDF 下载和在线打印，
 * 统一使用 SalesTeamGrossProfitService 的旧系统公式。</p>
 */
@RestController
public class SalesTeamGrossProfitController extends ControllerSupport {

    private final SalesTeamGrossProfitService service;

    /**
     * 构造团队预算毛利表 Controller。
     */
    public SalesTeamGrossProfitController(SalesTeamGrossProfitService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 查询团队预算毛利表预览数据。
     *
     * @param teamId 团队 ID
     * @return 预算毛利表结构化数据
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/sales/team/{teamId}/gross-profit/preview")
    public ApiResponse<SalesTeamGrossProfitPreviewResponse> preview(@PathVariable Long teamId) {
        return ApiResponse.ok(service.preview(teamId, currentTenantId()));
    }

    /**
     * 下载团队预算毛利表 Word 或 PDF 文件。
     *
     * @param teamId 团队 ID
     * @param format 文件格式，docx 或 pdf
     * @param response HTTP 响应
     */
    @OperationLog(module = "销售管理", type = "导出")
    @GetMapping("/sales/team/{teamId}/gross-profit/export")
    public void export(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "docx") String format,
            HttpServletResponse response
    ) throws IOException {
        SalesTeamGrossProfitService.ExportResult result = service.export(teamId, format, currentTenantId());
        ByteArrayOutputStream content = result.content();
        String encodedFilename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(result.contentType());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(content.size());
        content.writeTo(response.getOutputStream());
    }
}
