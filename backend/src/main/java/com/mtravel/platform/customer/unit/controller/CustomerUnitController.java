package com.mtravel.platform.customer.unit.controller;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.unit.dto.CustomerUnitCreateRequest;
import com.mtravel.platform.customer.unit.dto.CustomerUnitResponse;
import com.mtravel.platform.customer.unit.dto.CustomerUnitUpdateRequest;
import com.mtravel.platform.customer.unit.service.CustomerUnitService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantContextHolder;
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

/**
 * 客户单位接口。
 *
 * <p>客户单位是客户管理的主档入口，接口层只负责参数接收、租户解析和操作人解析；
 * 客户编码唯一、分类有效性和软删除等业务规则统一放在 Service 层。</p>
 */
@Validated
@RestController
@RequestMapping("/customer/unit")
public class CustomerUnitController {

    private final CustomerUnitService service;
    private final TenantProperties tenantProperties;

    public CustomerUnitController(CustomerUnitService service, TenantProperties tenantProperties) {
        this.service = service;
        this.tenantProperties = tenantProperties;
    }

    /**
     * 分页查询客户单位。
     *
     * @param keyword 客户名称、负责人或联系电话关键字。
     * @param customerCode 客户编码关键字。
     * @param categoryId 客户分类筛选。
     * @param status 客户状态筛选，可为空；传值时只允许 active / disabled。
     * @param province 省份筛选。
     * @param city 城市筛选。
     * @param district 区县筛选。
     * @param departmentName 部门筛选。
     * @param page 当前页，从 1 开始。
     * @param pageSize 每页条数，限制最大 200，避免一次返回过多数据。
     */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<CustomerUnitResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(),
                keyword,
                customerCode,
                categoryId,
                status,
                province,
                city,
                district,
                departmentName,
                page,
                pageSize
        ));
    }

    /** 查询单个客户单位详情。 */
    @OperationLog(module = "客户管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<CustomerUnitResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增客户单位。 */
    @OperationLog(module = "客户管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<CustomerUnitResponse> create(
            @Valid @RequestBody CustomerUnitCreateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改客户单位。 */
    @OperationLog(module = "客户管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<CustomerUnitResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody CustomerUnitUpdateRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除客户单位。 */
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
