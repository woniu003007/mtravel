package com.mtravel.platform.dispatch.vehiclequote.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteCalculateRequest;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteCalculateResponse;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleResponse;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleSaveRequest;
import com.mtravel.platform.dispatch.vehiclequote.service.VehicleQuoteRuleService;
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
 * 座位数报价规则接口。
 *
 * <p>供计调维护车队报价规则，并给产品团队安排用车弹窗提供参考价测算。</p>
 */
@Validated
@RestController
@RequestMapping("/dispatch/vehicle-quote-rules")
public class VehicleQuoteRuleController extends ControllerSupport {

    private final VehicleQuoteRuleService service;

    public VehicleQuoteRuleController(VehicleQuoteRuleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询座位数报价规则。
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<VehicleQuoteRuleResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, vehicleType, status, city, page, pageSize));
    }

    /**
     * 查询启用规则列表。
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<VehicleQuoteRuleResponse>> all(@RequestParam(required = false) String vehicleType) {
        return ApiResponse.ok(service.listActive(currentTenantId(), vehicleType));
    }

    /**
     * 新增座位数报价规则。
     */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/create")
    public ApiResponse<VehicleQuoteRuleResponse> create(
            @Valid @RequestBody VehicleQuoteRuleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 修改座位数报价规则。
     */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/update")
    public ApiResponse<VehicleQuoteRuleResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody VehicleQuoteRuleSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /**
     * 软删除座位数报价规则。
     */
    @OperationLog(module = "计调操作", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /**
     * 根据路书公里测算用车参考价。
     */
    @OperationLog(module = "计调操作", type = "查询")
    @PostMapping("/calculate")
    public ApiResponse<VehicleQuoteCalculateResponse> calculate(@Valid @RequestBody VehicleQuoteCalculateRequest request) {
        return ApiResponse.ok(service.calculate(request, currentTenantId()));
    }
}
