package com.mtravel.platform.customer.category.controller;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.dto.CustomerCategoryCreateRequest;
import com.mtravel.platform.customer.category.dto.CustomerCategoryResponse;
import com.mtravel.platform.customer.category.dto.CustomerCategoryUpdateRequest;
import com.mtravel.platform.customer.category.service.CustomerCategoryService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantContextHolder;
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
 * 客户分类接口。
 *
 * <p>客户分类属于客户主档的基础字典，前端主要用于客户列表筛选、客户表单下拉框、
 * 后续客户统计分组。接口层只负责参数接收和当前租户/操作人解析，具体业务规则放在 Service。</p>
 */
@Validated
@RestController
@RequestMapping("/customer/category")
public class CustomerCategoryController {

    private final CustomerCategoryService service;
    private final TenantProperties tenantProperties;

    public CustomerCategoryController(CustomerCategoryService service, TenantProperties tenantProperties) {
        this.service = service;
        this.tenantProperties = tenantProperties;
    }

    /**
     * 分页查询客户分类。
     *
     * @param keyword 分类名称关键字，可为空。
     * @param status 状态筛选，可为空；传值时只允许 active / disabled。
     * @param page 当前页，从 1 开始。
     * @param pageSize 每页条数，限制最大 200，避免一次返回过多数据。
     */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<CustomerCategoryResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, status, page, pageSize));
    }

    /**
     * 查询启用客户分类列表。
     *
     * <p>这个接口用于新增/编辑客户单位时的下拉框，只返回 active 分类。</p>
     */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<CustomerCategoryResponse>> all() {
        return ApiResponse.ok(service.listActive(currentTenantId()));
    }

    /** 查询单个客户分类详情。 */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<CustomerCategoryResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增客户分类。 */
    @OperationLog(module = "客户管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<CustomerCategoryResponse> create(
            @Valid @RequestBody CustomerCategoryCreateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改客户分类。 */
    @OperationLog(module = "客户管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<CustomerCategoryResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody CustomerCategoryUpdateRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除客户分类。 */
    @OperationLog(module = "客户管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    private Long currentTenantId() {
        return TenantContextHolder.getTenantId(tenantProperties.getDefaultTenantId());
    }

    private String currentOperator(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.username();
        }
        return "system";
    }
}
