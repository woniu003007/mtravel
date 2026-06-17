package com.mtravel.platform.contract.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.contract.dto.ContractResponse;
import com.mtravel.platform.contract.dto.ContractSaveRequest;
import com.mtravel.platform.contract.service.ContractService;
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

/** 统一合同接口，供客户管理和采购管理入口共同调用。 */
@Validated
@RestController
@RequestMapping("/contracts")
public class ContractController extends ControllerSupport {

    private final ContractService service;

    public ContractController(ContractService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询合同台账。 */
    @OperationLog(module = "合同管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<ContractResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(), keyword, contractType, status, customerId, supplierId, page, pageSize
        ));
    }

    /** 查询合同详情。 */
    @OperationLog(module = "合同管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<ContractResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 按合同类型预生成合同编号。 */
    @OperationLog(module = "合同管理", type = "查询")
    @GetMapping("/next-no")
    public ApiResponse<String> nextNo(@RequestParam String contractType) {
        return ApiResponse.ok(service.nextContractNo(currentTenantId(), contractType));
    }

    /** 新增合同并绑定已上传的合同文件。 */
    @OperationLog(module = "合同管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<ContractResponse> create(
            @Valid @RequestBody ContractSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改合同。 */
    @OperationLog(module = "合同管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<ContractResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody ContractSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除合同，保留历史业务引用。 */
    @OperationLog(module = "合同管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
