package com.mtravel.platform.enterprise.role.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRolePermissionResponse;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRolePermissionSaveRequest;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRoleResponse;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRoleSaveRequest;
import com.mtravel.platform.enterprise.role.service.EnterpriseRoleService;
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
 * 企业角色管理接口。
 *
 * <p>角色用于员工账号归属和权限入口配置。Controller 只负责参数接收、租户解析和响应包装。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/role")
public class EnterpriseRoleController extends ControllerSupport {

    private final EnterpriseRoleService service;

    public EnterpriseRoleController(EnterpriseRoleService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询企业角色。
     *
     * @param keyword 角色编码或角色名称关键字
     * @param status 状态筛选，可为空
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数，最大 200
     * @return 角色分页结果
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseRoleResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, status, page, pageSize));
    }

    /** 查询角色列表，用于员工角色下拉选择。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseRoleResponse>> all(
            @RequestParam(defaultValue = "false") Boolean includeDisabled
    ) {
        return ApiResponse.ok(service.listAll(currentTenantId(), Boolean.TRUE.equals(includeDisabled)));
    }

    /** 查询单个角色详情。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseRoleResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增企业角色。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseRoleResponse> create(
            @Valid @RequestBody EnterpriseRoleSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改企业角色。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseRoleResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseRoleSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除企业角色。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 查询角色已分配权限。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/permissions")
    public ApiResponse<List<EnterpriseRolePermissionResponse>> permissions(@RequestParam Long id) {
        return ApiResponse.ok(service.listPermissions(id, currentTenantId()));
    }

    /** 保存角色权限配置。 */
    @OperationLog(module = "企业资料", type = "权限配置")
    @PostMapping("/permissions")
    public ApiResponse<Void> savePermissions(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseRolePermissionSaveRequest request,
            Authentication authentication
    ) {
        service.savePermissions(id, request, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
