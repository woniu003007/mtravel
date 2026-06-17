package com.mtravel.platform.enterprise.department.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.department.dto.EnterpriseDepartmentResponse;
import com.mtravel.platform.enterprise.department.dto.EnterpriseDepartmentSaveRequest;
import com.mtravel.platform.enterprise.department.service.EnterpriseDepartmentService;
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
 * 企业部门管理接口。
 *
 * <p>部门是企业资料基础数据，Controller 只负责参数接收、租户解析和响应包装。
 * 部门名称唯一、上级部门校验、软删除保护等业务规则放在 Service。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/department")
public class EnterpriseDepartmentController extends ControllerSupport {

    private final EnterpriseDepartmentService service;

    public EnterpriseDepartmentController(
            EnterpriseDepartmentService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询企业部门。
     *
     * @param keyword 部门名称、编码、负责人或联系电话关键字。
     * @param status 状态筛选，可为空；传值时只允许 active / disabled。
     * @param parentId 上级部门筛选，可为空。
     * @param page 当前页，从 1 开始。
     * @param pageSize 每页条数，最大 200。
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseDepartmentResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, status, parentId, page, pageSize));
    }

    /** 查询部门列表，用于上级部门和员工所属部门下拉选择。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseDepartmentResponse>> all(
            @RequestParam(defaultValue = "false") Boolean includeDisabled
    ) {
        return ApiResponse.ok(service.listAll(currentTenantId(), Boolean.TRUE.equals(includeDisabled)));
    }

    /** 查询单个部门详情。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseDepartmentResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增企业部门。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseDepartmentResponse> create(
            @Valid @RequestBody EnterpriseDepartmentSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改企业部门。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseDepartmentResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseDepartmentSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除企业部门。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
