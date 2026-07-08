package com.mtravel.platform.finance.guideimprest.controller;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestApplyRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestCancelRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestDecisionRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestPaymentRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestPreviewResponse;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestResponse;
import com.mtravel.platform.finance.guideimprest.service.FinanceGuideImprestService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
 * 导游备用金接口。
 *
 * <p>计调通过预览和提交创建备用金申请，总经理在审批页处理申请，财务在审批通过后登记付款。</p>
 */
@Validated
@RestController
@RequestMapping("/finance/guide-imprests")
public class FinanceGuideImprestController extends ControllerSupport {

    private final FinanceGuideImprestService service;

    public FinanceGuideImprestController(FinanceGuideImprestService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 预览导游备用金计算结果。 */
    @OperationLog(module = "财务管理", type = "查询")
    @GetMapping("/preview")
    public ApiResponse<GuideImprestPreviewResponse> preview(
            @RequestParam Long teamId,
            @RequestParam Long guideId,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "公司加点率不能小于0") BigDecimal companyMarkupRate
    ) {
        return ApiResponse.ok(service.preview(currentTenantId(), teamId, guideId, companyMarkupRate));
    }

    /** 提交导游备用金申请。 */
    @OperationLog(module = "财务管理", type = "新增")
    @PostMapping("/submit")
    public ApiResponse<GuideImprestResponse> submit(
            @Valid @RequestBody GuideImprestApplyRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.submit(currentTenantId(), request, currentOperator(authentication)));
    }

    /** 分页查询导游备用金申请。 */
    @OperationLog(module = "财务管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<GuideImprestResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long guideId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, status, teamId, guideId, page, pageSize));
    }

    /** 查询导游备用金申请详情。 */
    @OperationLog(module = "财务管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<GuideImprestResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(currentTenantId(), id));
    }

    /** 总经理同意导游备用金申请。 */
    @OperationLog(module = "财务管理", type = "审批")
    @PostMapping("/approve")
    public ApiResponse<GuideImprestResponse> approve(
            @RequestParam Long id,
            @Valid @RequestBody GuideImprestDecisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.approve(
                currentTenantId(),
                id,
                request,
                currentOperator(authentication),
                currentRoles(authentication)
        ));
    }

    /** 总经理拒绝导游备用金申请。 */
    @OperationLog(module = "财务管理", type = "审批")
    @PostMapping("/reject")
    public ApiResponse<GuideImprestResponse> reject(
            @RequestParam Long id,
            @Valid @RequestBody GuideImprestDecisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.reject(
                currentTenantId(),
                id,
                request,
                currentOperator(authentication),
                currentRoles(authentication)
        ));
    }

    /** 财务登记导游备用金付款。 */
    @OperationLog(module = "财务管理", type = "付款")
    @PostMapping("/payment")
    public ApiResponse<GuideImprestResponse> registerPayment(
            @RequestParam Long id,
            @Valid @RequestBody GuideImprestPaymentRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.registerPayment(currentTenantId(), id, request, currentOperator(authentication)));
    }

    /** 作废未付款导游备用金申请。 */
    @OperationLog(module = "财务管理", type = "作废")
    @PostMapping("/cancel")
    public ApiResponse<GuideImprestResponse> cancel(
            @RequestParam Long id,
            @Valid @RequestBody GuideImprestCancelRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.cancel(currentTenantId(), id, request.cancelReason(), currentOperator(authentication)));
    }

    private List<String> currentRoles(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.roles();
        }
        return List.of();
    }
}
