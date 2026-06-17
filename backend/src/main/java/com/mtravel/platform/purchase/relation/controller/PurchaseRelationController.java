package com.mtravel.platform.purchase.relation.controller;

import com.mtravel.platform.common.ApiResponse; import com.mtravel.platform.common.ControllerSupport; import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationResponse; import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSaveRequest; import com.mtravel.platform.purchase.relation.service.PurchaseRelationService;
import com.mtravel.platform.system.log.web.OperationLog; import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid; import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min; import org.springframework.security.core.Authentication; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
/** 采购关系管理接口。 */
@Validated @RestController @RequestMapping("/purchase/relation")
public class PurchaseRelationController extends ControllerSupport{private final PurchaseRelationService service; public PurchaseRelationController(PurchaseRelationService service,TenantProperties tenantProperties){super(tenantProperties);this.service=service;}
 @OperationLog(module="采购管理", type="查询") @GetMapping("/page") public ApiResponse<PageResult<PurchaseRelationResponse>> page(@RequestParam(required=false) String keyword,@RequestParam(required=false) String resourceType,@RequestParam(required=false) String status,@RequestParam(required=false) Long supplierId,@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(200) long pageSize){return ApiResponse.ok(service.page(currentTenantId(),keyword,resourceType,status,supplierId,page,pageSize));}
 @OperationLog(module="采购管理", type="查询") @GetMapping("/detail") public ApiResponse<PurchaseRelationResponse> detail(@RequestParam Long id){return ApiResponse.ok(service.detail(id,currentTenantId()));}
 @OperationLog(module="采购管理", type="新增") @PostMapping("/create") public ApiResponse<PurchaseRelationResponse> create(@Valid @RequestBody PurchaseRelationSaveRequest request,Authentication authentication){return ApiResponse.ok(service.create(request,currentTenantId(),currentOperator(authentication)));}
 @OperationLog(module="采购管理", type="修改") @PostMapping("/update") public ApiResponse<PurchaseRelationResponse> update(@RequestParam Long id,@Valid @RequestBody PurchaseRelationSaveRequest request){return ApiResponse.ok(service.update(id,request,currentTenantId()));}
 @OperationLog(module="采购管理", type="删除") @PostMapping("/delete") public ApiResponse<Void> delete(@RequestParam Long id,Authentication authentication){service.delete(id,currentTenantId(),currentOperator(authentication));return ApiResponse.ok();}
}
