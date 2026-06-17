package com.mtravel.platform.enterprise.employee.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.employee.dto.EnterpriseEmployeeResponse;
import com.mtravel.platform.enterprise.employee.dto.EnterpriseEmployeeSaveRequest;
import com.mtravel.platform.enterprise.employee.service.EnterpriseEmployeeService;
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
 * 企业员工管理接口。
 *
 * <p>员工管理负责员工名录和登录账号一体维护。Controller 只负责参数接收、租户解析和响应包装。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/employee")
public class EnterpriseEmployeeController extends ControllerSupport {

    private final EnterpriseEmployeeService service;

    public EnterpriseEmployeeController(EnterpriseEmployeeService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询企业员工。
     *
     * @param keyword 员工编码、姓名、账号、电话关键字
     * @param departmentId 部门筛选
     * @param roleId 角色筛选
     * @param status 状态筛选
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数，最大 200
     * @return 员工分页结果
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseEmployeeResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, departmentId, roleId, status, page, pageSize));
    }

    /** 查询员工下拉列表，用于客户单位默认操作计调等业务选择。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseEmployeeResponse>> all(
            @RequestParam(defaultValue = "false") Boolean includeDisabled
    ) {
        return ApiResponse.ok(service.listAll(currentTenantId(), Boolean.TRUE.equals(includeDisabled)));
    }

    /** 查询单个员工详情。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseEmployeeResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增员工并创建登录账号，初始密码由业务服务统一设置。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseEmployeeResponse> create(
            @Valid @RequestBody EnterpriseEmployeeSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改员工资料，并同步登录账号基础信息。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseEmployeeResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseEmployeeSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 停用员工，并同步停用关联登录账号。 */
    @OperationLog(module = "企业资料", type = "停用")
    @PostMapping("/disable")
    public ApiResponse<Void> disable(@RequestParam Long id) {
        service.disable(id, currentTenantId());
        return ApiResponse.ok();
    }

    /** 重置员工登录密码为系统默认初始密码。 */
    @OperationLog(module = "企业资料", type = "重置密码")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestParam Long id) {
        service.resetPassword(id, currentTenantId());
        return ApiResponse.ok();
    }

    /** 软删除员工，并同步软删除关联登录账号。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
