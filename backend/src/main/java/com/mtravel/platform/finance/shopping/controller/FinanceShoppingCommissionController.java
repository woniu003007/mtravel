package com.mtravel.platform.finance.shopping.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionOverviewResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionRuleResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionRuleSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackLineResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackLineSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingSettlementCalculateRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingSettlementResponse;
import com.mtravel.platform.finance.shopping.service.FinanceShoppingCommissionService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物业绩和公司补佣接口。
 *
 * <p>提供团队购物规则覆盖、购物店反馈录入和购物业绩结算重新计算。接口只暴露业务动作，
 * 金额结果由后端服务统一计算。</p>
 */
@Validated
@RestController
@RequestMapping("/finance/shopping/team/{teamId}")
public class FinanceShoppingCommissionController extends ControllerSupport {

    private final FinanceShoppingCommissionService service;

    public FinanceShoppingCommissionController(
            FinanceShoppingCommissionService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 查询团队购物业绩和最新结算总览。 */
    @OperationLog(module = "财务管理", type = "查询")
    @GetMapping("/overview")
    public ApiResponse<ShoppingCommissionOverviewResponse> overview(@PathVariable Long teamId) {
        return ApiResponse.ok(service.overview(currentTenantId(), teamId));
    }

    /** 保存团队购物参考阶梯规则覆盖。 */
    @OperationLog(module = "财务管理", type = "修改")
    @PostMapping("/rule-override")
    public ApiResponse<ShoppingCommissionRuleResponse> saveTeamRuleOverride(
            @PathVariable Long teamId,
            @Valid @RequestBody ShoppingCommissionRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.saveTeamRuleOverride(
                currentTenantId(),
                teamId,
                request,
                currentOperator(authentication)
        ));
    }

    /** 保存单条购物店反馈明细。 */
    @OperationLog(module = "财务管理", type = "保存")
    @PostMapping("/feedback-lines")
    public ApiResponse<ShoppingFeedbackLineResponse> saveFeedbackLine(
            @PathVariable Long teamId,
            @Valid @RequestBody ShoppingFeedbackLineSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.saveFeedbackLine(
                currentTenantId(),
                teamId,
                request,
                currentOperator(authentication)
        ));
    }

    /** 作废单条购物店反馈明细。 */
    @OperationLog(module = "财务管理", type = "作废")
    @PostMapping("/feedback-lines/{id}/cancel")
    public ApiResponse<Void> cancelFeedbackLine(
            @PathVariable Long teamId,
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.cancelFeedbackLine(currentTenantId(), teamId, id, currentOperator(authentication));
        return ApiResponse.ok(null);
    }

    /** 重新计算团队购物业绩结算快照。 */
    @OperationLog(module = "财务管理", type = "计算")
    @PostMapping("/calculate")
    public ApiResponse<ShoppingSettlementResponse> calculateSettlement(
            @PathVariable Long teamId,
            @Valid @RequestBody(required = false) ShoppingSettlementCalculateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.calculateSettlement(
                currentTenantId(),
                teamId,
                request,
                currentOperator(authentication)
        ));
    }
}
