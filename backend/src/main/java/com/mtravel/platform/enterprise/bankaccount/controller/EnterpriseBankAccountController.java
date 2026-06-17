package com.mtravel.platform.enterprise.bankaccount.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.bankaccount.dto.EnterpriseBankAccountResponse;
import com.mtravel.platform.enterprise.bankaccount.dto.EnterpriseBankAccountSaveRequest;
import com.mtravel.platform.enterprise.bankaccount.service.EnterpriseBankAccountService;
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
 * 企业银行账号接口。
 *
 * <p>银行账号是企业资料基础模块，后续会被收款记录、付款记录、导游备用金和员工现金账权限复用。
 * Controller 只处理接口参数和当前租户/操作人解析，具体业务规则放在 Service。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/bank-account")
public class EnterpriseBankAccountController extends ControllerSupport {

    private final EnterpriseBankAccountService service;

    public EnterpriseBankAccountController(
            EnterpriseBankAccountService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询企业银行账号。
     *
     * @param keyword 开户行、户名、账号或其它说明关键字。
     * @param status 状态筛选，可为空；传值时只允许 active / disabled。
     * @param page 当前页，从 1 开始。
     * @param pageSize 每页条数，最大 200。
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseBankAccountResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, status, page, pageSize));
    }

    /** 查询启用银行账号，用于业务单据下拉选择。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseBankAccountResponse>> all() {
        return ApiResponse.ok(service.listActive(currentTenantId()));
    }

    /** 查询单个银行账号详情。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseBankAccountResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增企业银行账号。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseBankAccountResponse> create(
            @Valid @RequestBody EnterpriseBankAccountSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改企业银行账号。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseBankAccountResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseBankAccountSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 修改银行账号是否参与打印展示。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/print-enabled")
    public ApiResponse<Void> updatePrintEnabled(
            @RequestParam Long id,
            @RequestParam Boolean printEnabled
    ) {
        service.updatePrintEnabled(id, currentTenantId(), printEnabled);
        return ApiResponse.ok();
    }

    /** 软删除企业银行账号。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
