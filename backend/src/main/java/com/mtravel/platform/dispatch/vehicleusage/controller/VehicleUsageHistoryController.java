package com.mtravel.platform.dispatch.vehicleusage.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.dispatch.vehicleusage.dto.VehicleUsageHistoryRecordRequest;
import com.mtravel.platform.dispatch.vehicleusage.dto.VehicleUsageHistoryResponse;
import com.mtravel.platform.dispatch.vehicleusage.service.VehicleUsageHistoryService;
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
 * 用车历史候选接口。
 *
 * <p>供产品团队安排和后续正式团队安排页面复用，前端仍允许手动输入。</p>
 */
@Validated
@RestController
@RequestMapping("/dispatch/vehicle-usage-histories")
public class VehicleUsageHistoryController extends ControllerSupport {

    private final VehicleUsageHistoryService service;

    public VehicleUsageHistoryController(VehicleUsageHistoryService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 查询司机信息或车牌号历史候选。
     *
     * @param historyType 候选类型
     * @param keyword 搜索关键字
     * @param limit 返回数量上限
     * @return 历史候选列表
     */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping
    public ApiResponse<List<VehicleUsageHistoryResponse>> suggest(
            @RequestParam String historyType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit
    ) {
        return ApiResponse.ok(service.suggest(historyType, keyword, currentTenantId(), limit));
    }

    /**
     * 记录一次手动输入内容的使用。
     *
     * @param request 使用记录请求
     * @param authentication 当前登录认证信息
     * @return 空响应
     */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/record-use")
    public ApiResponse<Void> recordUse(
            @Valid @RequestBody VehicleUsageHistoryRecordRequest request,
            Authentication authentication
    ) {
        service.recordUse(request, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
