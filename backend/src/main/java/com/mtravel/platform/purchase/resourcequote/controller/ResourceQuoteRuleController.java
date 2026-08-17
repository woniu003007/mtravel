package com.mtravel.platform.purchase.resourcequote.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.resourcequote.dto.ResourceQuoteRuleResponse;
import com.mtravel.platform.purchase.resourcequote.dto.ResourceQuoteRuleSaveRequest;
import com.mtravel.platform.purchase.resourcequote.service.ResourceQuoteRuleService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通资源报价规则接口。
 *
 * <p>供采购和报价业务维护各资源类型在不同客户等级下的建议及最低加价口径。</p>
 */
@Validated
@RestController
@RequestMapping("/purchase/resource-quote-rules")
public class ResourceQuoteRuleController extends ControllerSupport {

    private final ResourceQuoteRuleService service;

    public ResourceQuoteRuleController(ResourceQuoteRuleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询普通资源报价规则。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<ResourceQuoteRuleResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @Pattern(
                    regexp = "hotel|scenic|vehicle|restaurant|guide|ground_agent|ticket|shopping|other",
                    message = "资源类型不合法"
            ) String resourceType,
            @RequestParam(required = false) @Positive Long customerLevelId,
            @RequestParam(required = false)
            @Pattern(regexp = "active|disabled", message = "普通资源报价规则状态不合法") String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(), keyword, resourceType, customerLevelId, status, page, pageSize
        ));
    }

    /** 新增普通资源报价规则。 */
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<ResourceQuoteRuleResponse> create(
            @Valid @RequestBody ResourceQuoteRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改普通资源报价规则。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<ResourceQuoteRuleResponse> update(
            @RequestParam @Positive Long id,
            @Valid @RequestBody ResourceQuoteRuleSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除普通资源报价规则。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam @Positive Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
