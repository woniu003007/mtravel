package com.mtravel.platform.purchase.relation.price.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceResponse;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceSaveRequest;
import com.mtravel.platform.purchase.relation.price.service.SupplierResourcePriceService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采购关系价格管理接口。
 *
 * <p>该接口从采购关系行内进入，维护项目类型对应的门市、同行和团队价格。</p>
 */
@Validated
@RestController
@RequestMapping("/purchase/relation/price")
public class SupplierResourcePriceController extends ControllerSupport {

    private final SupplierResourcePriceService service;

    public SupplierResourcePriceController(SupplierResourcePriceService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询采购关系下的价格明细。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<SupplierResourcePriceResponse>> page(
            @RequestParam(required = false) Long relationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), relationId, status, page, pageSize));
    }

    /** 新增价格明细。 */
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<SupplierResourcePriceResponse> create(
            @Valid @RequestBody SupplierResourcePriceSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改价格明细。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<SupplierResourcePriceResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody SupplierResourcePriceSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除价格明细。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
