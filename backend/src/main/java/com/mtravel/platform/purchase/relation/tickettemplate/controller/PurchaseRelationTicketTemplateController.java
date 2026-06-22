package com.mtravel.platform.purchase.relation.tickettemplate.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateHeaderResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateSaveRequest;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateFillModeResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TouristSystemFieldResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.service.PurchaseRelationTicketTemplateService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采购关系游客名单模板接口。
 *
 * <p>页面入口位于采购关系行内“模板配置”，用于维护景区票务游客 Excel 模板和字段映射。</p>
 */
@Validated
@RestController
@RequestMapping("/purchase/relation/ticket-template")
public class PurchaseRelationTicketTemplateController extends ControllerSupport {

    private final PurchaseRelationTicketTemplateService service;

    public PurchaseRelationTicketTemplateController(
            PurchaseRelationTicketTemplateService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 查询采购关系下的模板配置。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<TicketTemplateResponse> detail(@RequestParam Long relationId) {
        return ApiResponse.ok(service.detailByRelation(currentTenantId(), relationId));
    }

    /** 读取上传模板的 Excel 表头。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/headers")
    public ApiResponse<TicketTemplateHeaderResponse> headers(
            @RequestParam Long attachmentId,
            @RequestParam(defaultValue = "1") Integer headerRow
    ) {
        return ApiResponse.ok(service.headers(currentTenantId(), attachmentId, headerRow));
    }

    /** 返回游客系统字段选项。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/system-fields")
    public ApiResponse<List<TouristSystemFieldResponse>> systemFields() {
        return ApiResponse.ok(TouristSystemFieldResponse.all());
    }

    /** 返回模板列填充方式选项。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/fill-modes")
    public ApiResponse<List<TicketTemplateFillModeResponse>> fillModes() {
        return ApiResponse.ok(TicketTemplateFillModeResponse.all());
    }

    /** 保存游客名单模板配置。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/save")
    public ApiResponse<TicketTemplateResponse> save(
            @Valid @RequestBody TicketTemplateSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.save(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 删除采购关系下的游客名单模板配置。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long relationId, Authentication authentication) {
        service.deleteByRelation(currentTenantId(), relationId, currentOperator(authentication));
        return ApiResponse.ok();
    }
}
