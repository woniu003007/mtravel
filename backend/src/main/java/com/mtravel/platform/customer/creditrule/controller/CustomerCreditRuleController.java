package com.mtravel.platform.customer.creditrule.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.creditrule.dto.CustomerCreditRuleResponse;
import com.mtravel.platform.customer.creditrule.dto.CustomerCreditRuleSaveRequest;
import com.mtravel.platform.customer.creditrule.service.CustomerCreditRuleService;
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
 * 客户授信规则接口。
 *
 * <p>供客户管理维护不同客户等级的默认授信额度、账期及超额审批通知规则。</p>
 */
@Validated
@RestController
@RequestMapping("/customer/credit-rules")
public class CustomerCreditRuleController extends ControllerSupport {

    private final CustomerCreditRuleService service;

    public CustomerCreditRuleController(CustomerCreditRuleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询客户授信规则。 */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<CustomerCreditRuleResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Positive Long customerLevelId,
            @RequestParam(required = false)
            @Pattern(regexp = "active|disabled", message = "客户授信规则状态不合法") String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, customerLevelId, status, page, pageSize));
    }

    /** 新增客户授信规则。 */
    @OperationLog(module = "客户管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<CustomerCreditRuleResponse> create(
            @Valid @RequestBody CustomerCreditRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改客户授信规则。 */
    @OperationLog(module = "客户管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<CustomerCreditRuleResponse> update(
            @RequestParam @Positive Long id,
            @Valid @RequestBody CustomerCreditRuleSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除客户授信规则。 */
    @OperationLog(module = "客户管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam @Positive Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
