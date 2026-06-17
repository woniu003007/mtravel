package com.mtravel.platform.purchase.supplier.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.supplier.dto.SupplierResponse;
import com.mtravel.platform.purchase.supplier.dto.SupplierSaveRequest;
import com.mtravel.platform.purchase.supplier.service.SupplierService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid; import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min; import java.util.List;
import org.springframework.security.core.Authentication; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;

/** 供应商管理接口。 */
@Validated @RestController @RequestMapping("/purchase/supplier")
public class SupplierController extends ControllerSupport{ private final SupplierService service; public SupplierController(SupplierService service,TenantProperties tenantProperties){super(tenantProperties);this.service=service;}
 @OperationLog(module="采购管理", type="查询") @GetMapping("/page") public ApiResponse<PageResult<SupplierResponse>> page(@RequestParam(required=false) String keyword,@RequestParam(required=false) String category,@RequestParam(required=false) String status,@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(200) long pageSize){return ApiResponse.ok(service.page(currentTenantId(),keyword,category,status,page,pageSize));}
 @OperationLog(module="采购管理", type="查询") @GetMapping("/all") public ApiResponse<List<SupplierResponse>> all(@RequestParam(required=false) String category){return ApiResponse.ok(service.listActive(currentTenantId(),category));}
 @OperationLog(module="采购管理", type="查询") @GetMapping("/detail") public ApiResponse<SupplierResponse> detail(@RequestParam Long id){return ApiResponse.ok(service.detail(id,currentTenantId()));}
 @OperationLog(module="采购管理", type="新增") @PostMapping("/create") public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierSaveRequest request,Authentication authentication){return ApiResponse.ok(service.create(request,currentTenantId(),currentOperator(authentication)));}
 @OperationLog(module="采购管理", type="修改") @PostMapping("/update") public ApiResponse<SupplierResponse> update(@RequestParam Long id,@Valid @RequestBody SupplierSaveRequest request){return ApiResponse.ok(service.update(id,request,currentTenantId()));}
 @OperationLog(module="采购管理", type="删除") @PostMapping("/delete") public ApiResponse<Void> delete(@RequestParam Long id,Authentication authentication){service.delete(id,currentTenantId(),currentOperator(authentication));return ApiResponse.ok();}
}
