package com.mtravel.platform.purchase.resource.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceBindingResponse;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceResponse;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceSaveRequest;
import com.mtravel.platform.purchase.resource.service.PurchaseResourceService;
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
 * 采购资源总览接口。
 *
 * <p>资源总览用于维护资源主档。绑定供应商时只创建采购关系，不在资源主档里保存采购价格，
 * 避免资源信息和价格关系混在同一张表里。</p>
 */
@Validated
@RestController
@RequestMapping("/purchase/resource")
public class PurchaseResourceController extends ControllerSupport {

    private final PurchaseResourceService service;

    public PurchaseResourceController(PurchaseResourceService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询资源总览。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<PurchaseResourceResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(),
                keyword,
                resourceType,
                province,
                city,
                district,
                status,
                page,
                pageSize
        ));
    }

    /** 查询资源详情。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<PurchaseResourceResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 查询资源已经绑定的供应商关系。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/bindings")
    public ApiResponse<List<PurchaseResourceBindingResponse>> bindings(@RequestParam Long resourceId) {
        return ApiResponse.ok(service.bindings(resourceId, currentTenantId()));
    }

    /** 新增采购资源。 */
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<PurchaseResourceResponse> create(
            @Valid @RequestBody PurchaseResourceSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改采购资源。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<PurchaseResourceResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody PurchaseResourceSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除采购资源。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
