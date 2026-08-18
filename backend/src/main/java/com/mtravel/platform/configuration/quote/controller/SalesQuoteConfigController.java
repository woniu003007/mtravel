package com.mtravel.platform.configuration.quote.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalConfigRequest;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalConfigResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGroundAgentRuleResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGroundAgentRuleSaveRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideLevelResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideLevelSaveRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideRuleResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideRuleSaveRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteResourceRuleResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteResourceRuleSaveRequest;
import com.mtravel.platform.configuration.quote.service.SalesQuoteConfigService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * 配置管理下的销售报价配置接口。
 */
@Validated
@RestController
@RequestMapping("/configuration/quote")
public class SalesQuoteConfigController extends ControllerSupport {

    private final SalesQuoteConfigService service;

    public SalesQuoteConfigController(SalesQuoteConfigService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询普通资源报价规则。 */
    @OperationLog(module = "配置管理", type = "查询")
    @GetMapping("/resource-rules/page")
    public ApiResponse<PageResult<SalesQuoteResourceRuleResponse>> resourceRulePage(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long customerCategoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.resourceRulePage(currentTenantId(), resourceType, customerCategoryId, status, page, pageSize));
    }

    /** 新增普通资源报价规则。 */
    @OperationLog(module = "配置管理", type = "新增")
    @PostMapping("/resource-rules/create")
    public ApiResponse<SalesQuoteResourceRuleResponse> createResourceRule(
            @Valid @RequestBody SalesQuoteResourceRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createResourceRule(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改普通资源报价规则。 */
    @OperationLog(module = "配置管理", type = "修改")
    @PostMapping("/resource-rules/update")
    public ApiResponse<SalesQuoteResourceRuleResponse> updateResourceRule(
            @RequestParam Long id,
            @Valid @RequestBody SalesQuoteResourceRuleSaveRequest request
    ) {
        return ApiResponse.ok(service.updateResourceRule(id, request, currentTenantId()));
    }

    /** 软删除普通资源报价规则。 */
    @OperationLog(module = "配置管理", type = "删除")
    @PostMapping("/resource-rules/delete")
    public ApiResponse<Void> deleteResourceRule(@RequestParam Long id, Authentication authentication) {
        service.deleteResourceRule(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询导游等级。 */
    @OperationLog(module = "配置管理", type = "查询")
    @GetMapping("/guide-levels/page")
    public ApiResponse<PageResult<SalesQuoteGuideLevelResponse>> guideLevelPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.guideLevelPage(currentTenantId(), keyword, status, page, pageSize));
    }

    /** 查询启用导游等级。 */
    @OperationLog(module = "配置管理", type = "查询")
    @GetMapping("/guide-levels/all")
    public ApiResponse<List<SalesQuoteGuideLevelResponse>> activeGuideLevels() {
        return ApiResponse.ok(service.activeGuideLevels(currentTenantId()));
    }

    /** 新增导游等级。 */
    @OperationLog(module = "配置管理", type = "新增")
    @PostMapping("/guide-levels/create")
    public ApiResponse<SalesQuoteGuideLevelResponse> createGuideLevel(
            @Valid @RequestBody SalesQuoteGuideLevelSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createGuideLevel(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改导游等级。 */
    @OperationLog(module = "配置管理", type = "修改")
    @PostMapping("/guide-levels/update")
    public ApiResponse<SalesQuoteGuideLevelResponse> updateGuideLevel(
            @RequestParam Long id,
            @Valid @RequestBody SalesQuoteGuideLevelSaveRequest request
    ) {
        return ApiResponse.ok(service.updateGuideLevel(id, request, currentTenantId()));
    }

    /** 软删除导游等级。 */
    @OperationLog(module = "配置管理", type = "删除")
    @PostMapping("/guide-levels/delete")
    public ApiResponse<Void> deleteGuideLevel(@RequestParam Long id, Authentication authentication) {
        service.deleteGuideLevel(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询导游报价规则。 */
    @OperationLog(module = "配置管理", type = "查询")
    @GetMapping("/guide-rules/page")
    public ApiResponse<PageResult<SalesQuoteGuideRuleResponse>> guideRulePage(
            @RequestParam(required = false) Long guideLevelId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.guideRulePage(currentTenantId(), guideLevelId, status, page, pageSize));
    }

    /** 新增导游报价规则。 */
    @OperationLog(module = "配置管理", type = "新增")
    @PostMapping("/guide-rules/create")
    public ApiResponse<SalesQuoteGuideRuleResponse> createGuideRule(
            @Valid @RequestBody SalesQuoteGuideRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createGuideRule(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改导游报价规则。 */
    @OperationLog(module = "配置管理", type = "修改")
    @PostMapping("/guide-rules/update")
    public ApiResponse<SalesQuoteGuideRuleResponse> updateGuideRule(
            @RequestParam Long id,
            @Valid @RequestBody SalesQuoteGuideRuleSaveRequest request
    ) {
        return ApiResponse.ok(service.updateGuideRule(id, request, currentTenantId()));
    }

    /** 软删除导游报价规则。 */
    @OperationLog(module = "配置管理", type = "删除")
    @PostMapping("/guide-rules/delete")
    public ApiResponse<Void> deleteGuideRule(@RequestParam Long id, Authentication authentication) {
        service.deleteGuideRule(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询地接报价规则。 */
    @OperationLog(module = "配置管理", type = "查询")
    @GetMapping("/ground-agent-rules/page")
    public ApiResponse<PageResult<SalesQuoteGroundAgentRuleResponse>> groundAgentRulePage(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.groundAgentRulePage(currentTenantId(), status, page, pageSize));
    }

    /** 新增地接报价规则。 */
    @OperationLog(module = "配置管理", type = "新增")
    @PostMapping("/ground-agent-rules/create")
    public ApiResponse<SalesQuoteGroundAgentRuleResponse> createGroundAgentRule(
            @Valid @RequestBody SalesQuoteGroundAgentRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createGroundAgentRule(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改地接报价规则。 */
    @OperationLog(module = "配置管理", type = "修改")
    @PostMapping("/ground-agent-rules/update")
    public ApiResponse<SalesQuoteGroundAgentRuleResponse> updateGroundAgentRule(
            @RequestParam Long id,
            @Valid @RequestBody SalesQuoteGroundAgentRuleSaveRequest request
    ) {
        return ApiResponse.ok(service.updateGroundAgentRule(id, request, currentTenantId()));
    }

    /** 软删除地接报价规则。 */
    @OperationLog(module = "配置管理", type = "删除")
    @PostMapping("/ground-agent-rules/delete")
    public ApiResponse<Void> deleteGroundAgentRule(@RequestParam Long id, Authentication authentication) {
        service.deleteGroundAgentRule(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 查询统一报价审批配置。 */
    @OperationLog(module = "配置管理", type = "查询")
    @GetMapping("/approval-config")
    public ApiResponse<QuoteApprovalConfigResponse> approvalConfig() {
        return ApiResponse.ok(service.approvalConfig(currentTenantId()));
    }

    /** 保存统一报价审批配置。 */
    @OperationLog(module = "配置管理", type = "修改")
    @PostMapping("/approval-config/save")
    public ApiResponse<QuoteApprovalConfigResponse> saveApprovalConfig(
            @Valid @RequestBody QuoteApprovalConfigRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.saveApprovalConfig(request, currentTenantId(), currentOperator(authentication)));
    }
}
