package com.mtravel.platform.customer.risk.controller;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalApplyRequest;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalDecisionRequest;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalResponse;
import com.mtravel.platform.customer.risk.dto.CustomerRiskCheckResponse;
import com.mtravel.platform.customer.risk.service.CustomerRiskApprovalService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
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
 * 客户风控审批接口。
 *
 * <p>收客页通过 check/apply 判断客户是否需要总经理审批；总经理审批页通过 page/detail/approve/reject
 * 处理审批单。Controller 不直接判断合同或授信规则。</p>
 */
@Validated
@RestController
@RequestMapping("/customer/risk-approval")
public class CustomerRiskApprovalController extends ControllerSupport {

    private final CustomerRiskApprovalService service;

    public CustomerRiskApprovalController(CustomerRiskApprovalService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 校验客户合同到期和授信超限风险。 */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/check")
    public ApiResponse<CustomerRiskCheckResponse> check(
            @RequestParam Long customerId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "0") BigDecimal requestedAmount
    ) {
        return ApiResponse.ok(service.check(currentTenantId(), customerId, teamId, orderId, requestedAmount));
    }

    /** 提交客户风控审批申请。 */
    @OperationLog(module = "客户管理", type = "新增")
    @PostMapping("/apply")
    public ApiResponse<CustomerRiskApprovalResponse> apply(
            @Valid @RequestBody CustomerRiskApprovalApplyRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.apply(currentTenantId(), request, currentOperator(authentication)));
    }

    /** 分页查询客户风控审批申请。 */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<CustomerRiskApprovalResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(),
                keyword,
                status,
                customerId,
                teamId,
                orderId,
                page,
                pageSize
        ));
    }

    /** 查询客户风控审批详情。 */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<CustomerRiskApprovalResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(currentTenantId(), id));
    }

    /** 总经理或管理员同意客户风控审批。 */
    @OperationLog(module = "客户管理", type = "审批")
    @PostMapping("/approve")
    public ApiResponse<CustomerRiskApprovalResponse> approve(
            @RequestParam Long id,
            @Valid @RequestBody CustomerRiskApprovalDecisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.approve(
                currentTenantId(),
                id,
                request.approvalRemark(),
                currentOperator(authentication),
                currentRoles(authentication)
        ));
    }

    /** 总经理或管理员拒绝客户风控审批。 */
    @OperationLog(module = "客户管理", type = "审批")
    @PostMapping("/reject")
    public ApiResponse<CustomerRiskApprovalResponse> reject(
            @RequestParam Long id,
            @Valid @RequestBody CustomerRiskApprovalDecisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.reject(
                currentTenantId(),
                id,
                request.approvalRemark(),
                currentOperator(authentication),
                currentRoles(authentication)
        ));
    }

    private List<String> currentRoles(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.roles();
        }
        return List.of();
    }
}
