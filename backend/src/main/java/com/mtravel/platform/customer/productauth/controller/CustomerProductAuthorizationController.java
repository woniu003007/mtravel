package com.mtravel.platform.customer.productauth.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.productauth.dto.CustomerProductAuthorizationResponse;
import com.mtravel.platform.customer.productauth.dto.CustomerProductAuthorizationSaveRequest;
import com.mtravel.platform.customer.productauth.service.CustomerProductAuthorizationService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 客户可售产品授权接口。 */
@Validated @RestController @RequestMapping("/customer/product-auth")
public class CustomerProductAuthorizationController extends ControllerSupport{
 private final CustomerProductAuthorizationService service; public CustomerProductAuthorizationController(CustomerProductAuthorizationService service,TenantProperties tenantProperties){super(tenantProperties);this.service=service;}
 @OperationLog(module="客户管理", type="查询") @GetMapping("/page") public ApiResponse<PageResult<CustomerProductAuthorizationResponse>> page(@RequestParam(required=false) String keyword,@RequestParam(required=false) String status,@RequestParam(required=false) Long customerId,@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(200) long pageSize){return ApiResponse.ok(service.page(currentTenantId(),keyword,status,customerId,page,pageSize));}
 @OperationLog(module="客户管理", type="查询") @GetMapping("/detail") public ApiResponse<CustomerProductAuthorizationResponse> detail(@RequestParam Long id){return ApiResponse.ok(service.detail(id,currentTenantId()));}
 @OperationLog(module="客户管理", type="新增") @PostMapping("/create") public ApiResponse<CustomerProductAuthorizationResponse> create(@Valid @RequestBody CustomerProductAuthorizationSaveRequest request, Authentication authentication){return ApiResponse.ok(service.create(request,currentTenantId(),currentOperator(authentication)));}
 @OperationLog(module="客户管理", type="修改") @PostMapping("/update") public ApiResponse<CustomerProductAuthorizationResponse> update(@RequestParam Long id,@Valid @RequestBody CustomerProductAuthorizationSaveRequest request){return ApiResponse.ok(service.update(id,request,currentTenantId()));}
 @OperationLog(module="客户管理", type="删除") @PostMapping("/delete") public ApiResponse<Void> delete(@RequestParam Long id,Authentication authentication){service.delete(id,currentTenantId(),currentOperator(authentication));return ApiResponse.ok();}
}
