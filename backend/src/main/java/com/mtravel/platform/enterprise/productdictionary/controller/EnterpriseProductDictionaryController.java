package com.mtravel.platform.enterprise.productdictionary.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.productdictionary.dto.EnterpriseProductDictionaryResponse;
import com.mtravel.platform.enterprise.productdictionary.dto.EnterpriseProductDictionarySaveRequest;
import com.mtravel.platform.enterprise.productdictionary.service.EnterpriseProductDictionaryService;
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
 * 产品字典管理接口。
 *
 * <p>该接口支撑企业资料中的产品字典页面，也给后续销售产品模板提供业务类型、
 * 接待标准和产品主题下拉选项。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/product-dictionary")
public class EnterpriseProductDictionaryController extends ControllerSupport {

    private final EnterpriseProductDictionaryService service;

    public EnterpriseProductDictionaryController(
            EnterpriseProductDictionaryService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /**
     * 分页查询产品字典。
     *
     * @param dictType 字典类型筛选
     * @param keyword 字典名称关键字
     * @param status 启停状态
     * @param page 当前页码
     * @param pageSize 每页数量
     * @return 产品字典分页结果
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseProductDictionaryResponse>> page(
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(currentTenantId(), dictType, keyword, status, page, pageSize));
    }

    /**
     * 查询启用产品字典。
     *
     * @param dictType 可选字典类型
     * @return 启用字典列表
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseProductDictionaryResponse>> all(@RequestParam(required = false) String dictType) {
        return ApiResponse.ok(service.listActive(currentTenantId(), dictType));
    }

    /**
     * 查询产品字典详情。
     *
     * @param id 字典ID
     * @return 产品字典详情
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseProductDictionaryResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /**
     * 新增产品字典。
     *
     * @param request 保存请求
     * @param authentication 当前登录认证信息，用于记录创建人
     * @return 新增后的字典详情
     */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseProductDictionaryResponse> create(
            @Valid @RequestBody EnterpriseProductDictionarySaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 修改产品字典。
     *
     * @param id 字典ID
     * @param request 保存请求
     * @return 修改后的字典详情
     */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseProductDictionaryResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseProductDictionarySaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /**
     * 软删除产品字典。
     *
     * @param id 字典ID
     * @param authentication 当前登录认证信息，用于记录删除人
     * @return 空响应
     */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
