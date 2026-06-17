package com.mtravel.platform.customer.credit.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.credit.dto.CustomerCreditAccountResponse;
import com.mtravel.platform.customer.credit.dto.CustomerCreditAccountSaveRequest;
import com.mtravel.platform.customer.credit.service.CustomerCreditAccountService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 客户授信与实时应收接口。 */
@Validated @RestController @RequestMapping("/customer/credit")
public class CustomerCreditAccountController extends ControllerSupport {
    private final CustomerCreditAccountService service;
    public CustomerCreditAccountController(CustomerCreditAccountService service, TenantProperties tenantProperties){super(tenantProperties);this.service=service;}
    /** 分页查询客户授信账户。 */
    @OperationLog(module="客户管理", type="查询") @GetMapping("/page")
    public ApiResponse<PageResult<CustomerCreditAccountResponse>> page(@RequestParam(required=false) String keyword,@RequestParam(required=false) String status,@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(200) long pageSize){return ApiResponse.ok(service.page(currentTenantId(),keyword,status,page,pageSize));}
    /** 查询授信账户详情。 */
    @OperationLog(module="客户管理", type="查询") @GetMapping("/detail") public ApiResponse<CustomerCreditAccountResponse> detail(@RequestParam Long id){return ApiResponse.ok(service.detail(id,currentTenantId()));}
    /** 新增授信账户。 */
    @OperationLog(module="客户管理", type="新增") @PostMapping("/create") public ApiResponse<CustomerCreditAccountResponse> create(@Valid @RequestBody CustomerCreditAccountSaveRequest request, Authentication authentication){return ApiResponse.ok(service.create(request,currentTenantId(),currentOperator(authentication)));}
    /** 修改授信账户。 */
    @OperationLog(module="客户管理", type="修改") @PostMapping("/update") public ApiResponse<CustomerCreditAccountResponse> update(@RequestParam Long id,@Valid @RequestBody CustomerCreditAccountSaveRequest request){return ApiResponse.ok(service.update(id,request,currentTenantId()));}
    /** 软删除授信账户。 */
    @OperationLog(module="客户管理", type="删除") @PostMapping("/delete") public ApiResponse<Void> delete(@RequestParam Long id,Authentication authentication){service.delete(id,currentTenantId(),currentOperator(authentication));return ApiResponse.ok();}
}
