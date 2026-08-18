package com.mtravel.platform.purchase.relation.price.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceResponse;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceSaveRequest;
import com.mtravel.platform.purchase.relation.price.service.SupplierResourcePriceService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "采购管理-供应商资源价格", description = "维护采购关系下不同项目类型的门市价、同行价和团队价。")
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
    @Operation(summary = "分页查询采购关系价格", description = "按采购关系、状态和分页条件查询供应商资源价格明细。")
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<SupplierResourcePriceResponse>> page(
            @Parameter(description = "采购关系 ID，不传则查询当前租户下全部关系价格")
            @RequestParam(required = false) Long relationId,
            @Parameter(description = "价格状态：active 启用，disabled 停用")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码，从 1 开始")
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，最大 200")
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), relationId, status, page, pageSize));
    }

    /** 新增价格明细。 */
    @Operation(summary = "新增采购关系价格", description = "给指定采购关系新增一个项目类型价格，保存门市价、同行价、团队价和价格说明。")
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<SupplierResourcePriceResponse> create(
            @Valid @RequestBody SupplierResourcePriceSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改价格明细。 */
    @Operation(summary = "修改采购关系价格", description = "按价格明细 ID 修改采购关系下的项目价格。")
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<SupplierResourcePriceResponse> update(
            @Parameter(description = "价格明细 ID")
            @RequestParam Long id,
            @Valid @RequestBody SupplierResourcePriceSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除价格明细。 */
    @Operation(summary = "删除采购关系价格", description = "按价格明细 ID 软删除采购关系价格，不物理删除数据库记录。")
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(
            @Parameter(description = "价格明细 ID")
            @RequestParam Long id,
            Authentication authentication
    ) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
