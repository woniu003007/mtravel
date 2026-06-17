package com.mtravel.platform.enterprise.companyinfo.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.enterprise.companyinfo.dto.EnterpriseCompanyInfoResponse;
import com.mtravel.platform.enterprise.companyinfo.dto.EnterpriseCompanyInfoSaveRequest;
import com.mtravel.platform.enterprise.companyinfo.service.EnterpriseCompanyInfoService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业公司信息接口。
 *
 * <p>Controller 只负责当前租户和操作人解析。公司信息保存、单租户单记录规则和状态校验
 * 由 Service 处理，避免企业资料页面和合同页面各自实现一套规则。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/company-info")
public class EnterpriseCompanyInfoController extends ControllerSupport {

    private final EnterpriseCompanyInfoService service;

    public EnterpriseCompanyInfoController(
            EnterpriseCompanyInfoService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 查询当前租户公司信息；未维护时 data 返回 null，前端继续展示空表单。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/current")
    public ApiResponse<EnterpriseCompanyInfoResponse> current() {
        return ApiResponse.ok(service.current(currentTenantId()));
    }

    /** 新增或修改当前租户公司信息。 */
    @OperationLog(module = "企业资料", type = "保存")
    @PostMapping("/save")
    public ApiResponse<EnterpriseCompanyInfoResponse> save(
            @Valid @RequestBody EnterpriseCompanyInfoSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.save(request, currentTenantId(), currentOperator(authentication)));
    }
}
