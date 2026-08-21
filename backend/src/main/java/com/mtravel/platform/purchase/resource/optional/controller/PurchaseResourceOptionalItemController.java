package com.mtravel.platform.purchase.resource.optional.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.purchase.resource.optional.dto.PurchaseResourceOptionalItemResponse;
import com.mtravel.platform.purchase.resource.optional.dto.PurchaseResourceOptionalItemSaveRequest;
import com.mtravel.platform.purchase.resource.optional.service.PurchaseResourceOptionalItemService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 景区资源自费项目主档接口。 */
@RestController
@RequestMapping("/purchase/resource/{resourceId}/optional-items")
public class PurchaseResourceOptionalItemController extends ControllerSupport {
    private final PurchaseResourceOptionalItemService service;
    public PurchaseResourceOptionalItemController(PurchaseResourceOptionalItemService service, TenantProperties tenantProperties) { super(tenantProperties); this.service = service; }
    @OperationLog(module = "采购管理", type = "查询") @GetMapping
    public ApiResponse<List<PurchaseResourceOptionalItemResponse>> list(@PathVariable Long resourceId) { return ApiResponse.ok(service.list(currentTenantId(), resourceId)); }
    @OperationLog(module = "采购管理", type = "新增") @PostMapping
    public ApiResponse<PurchaseResourceOptionalItemResponse> create(@PathVariable Long resourceId, @Valid @RequestBody PurchaseResourceOptionalItemSaveRequest request, Authentication authentication) { return ApiResponse.ok(service.create(currentTenantId(), resourceId, request, currentOperator(authentication))); }
    @OperationLog(module = "采购管理", type = "修改") @PostMapping("/{optionalItemId}")
    public ApiResponse<PurchaseResourceOptionalItemResponse> update(@PathVariable Long resourceId, @PathVariable Long optionalItemId, @Valid @RequestBody PurchaseResourceOptionalItemSaveRequest request) { return ApiResponse.ok(service.update(currentTenantId(), resourceId, optionalItemId, request)); }
    @OperationLog(module = "采购管理", type = "删除") @PostMapping("/{optionalItemId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long resourceId, @PathVariable Long optionalItemId, Authentication authentication) { service.delete(currentTenantId(), resourceId, optionalItemId, currentOperator(authentication)); return ApiResponse.ok(); }
}
