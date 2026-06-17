package com.mtravel.platform.enterprise.expenseitem.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.expenseitem.dto.EnterpriseExpenseItemResponse;
import com.mtravel.platform.enterprise.expenseitem.dto.EnterpriseExpenseItemSaveRequest;
import com.mtravel.platform.enterprise.expenseitem.service.EnterpriseExpenseItemService;
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
 * 费用项目管理接口。
 *
 * <p>该接口支撑企业资料中的费用项目页面，也给采购价格管理提供按资源类型过滤的项目下拉。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/expense-item")
public class EnterpriseExpenseItemController extends ControllerSupport {

    private final EnterpriseExpenseItemService service;

    public EnterpriseExpenseItemController(EnterpriseExpenseItemService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询费用项目。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseExpenseItemResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), keyword, resourceType, status, page, pageSize));
    }

    /** 查询某个资源类型下启用费用项目。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseExpenseItemResponse>> all(@RequestParam(required = false) String resourceType) {
        return ApiResponse.ok(service.listActiveByResourceType(currentTenantId(), resourceType));
    }

    /** 查询费用项目详情。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseExpenseItemResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增费用项目。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseExpenseItemResponse> create(
            @Valid @RequestBody EnterpriseExpenseItemSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改费用项目。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseExpenseItemResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseExpenseItemSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 软删除费用项目。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
