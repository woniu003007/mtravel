package com.mtravel.platform.enterprise.guide.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideResponse;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideSaveRequest;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideTagResponse;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideTagSaveRequest;
import com.mtravel.platform.enterprise.guide.service.EnterpriseGuideService;
import com.mtravel.platform.enterprise.guide.service.EnterpriseGuideTagService;
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
 * 企业导游管理接口。
 *
 * <p>导游管理负责导游档案、证件、联系方式、企业码状态和结算账号维护。
 * Controller 只负责参数接收、租户解析和响应包装，查重、软删除和状态规则放在 Service。</p>
 */
@Validated
@RestController
@RequestMapping("/enterprise/guide")
public class EnterpriseGuideController extends ControllerSupport {

    private final EnterpriseGuideService service;
    private final EnterpriseGuideTagService tagService;

    public EnterpriseGuideController(
            EnterpriseGuideService service,
            EnterpriseGuideTagService tagService,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
        this.tagService = tagService;
    }

    /**
     * 分页查询导游档案。
     *
     * @param keyword 导游编码、名称、用户名、证件号或电话关键字
     * @param enterpriseCodeStatus 企业码状态筛选
     * @param status 导游状态筛选
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数，最大 200
     */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnterpriseGuideResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String enterpriseCodeStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long guideManagerEmployeeId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(),
                keyword,
                enterpriseCodeStatus,
                status,
                guideManagerEmployeeId,
                tagId,
                page,
                pageSize
        ));
    }

    /** 查询导游列表，用于团队安排和后续导游排班下拉选择。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/all")
    public ApiResponse<List<EnterpriseGuideResponse>> all(
            @RequestParam(defaultValue = "false") Boolean includeDisabled
    ) {
        return ApiResponse.ok(service.listAll(currentTenantId(), Boolean.TRUE.equals(includeDisabled)));
    }

    /** 查询单个导游档案详情。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<EnterpriseGuideResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 发送企业码邀请，记录已获取签约链接状态和邀请时间。 */
    @OperationLog(module = "企业资料", type = "企业码邀请")
    @PostMapping("/send-enterprise-code-invite")
    public ApiResponse<Void> sendEnterpriseCodeInvite(@RequestParam Long id) {
        service.sendEnterpriseCodeInvite(id, currentTenantId());
        return ApiResponse.ok();
    }

    /** 新增导游档案。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/create")
    public ApiResponse<EnterpriseGuideResponse> create(
            @Valid @RequestBody EnterpriseGuideSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改导游档案。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/update")
    public ApiResponse<EnterpriseGuideResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseGuideSaveRequest request
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId()));
    }

    /** 停用导游档案。 */
    @OperationLog(module = "企业资料", type = "停用")
    @PostMapping("/disable")
    public ApiResponse<Void> disable(@RequestParam Long id) {
        service.disable(id, currentTenantId());
        return ApiResponse.ok();
    }

    /** 软删除导游档案。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询导游标签。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/tags/page")
    public ApiResponse<PageResult<EnterpriseGuideTagResponse>> tagPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(tagService.page(currentTenantId(), keyword, status, page, pageSize));
    }

    /** 查询启用导游标签，用于导游档案标签多选。 */
    @OperationLog(module = "企业资料", type = "查询")
    @GetMapping("/tags/all")
    public ApiResponse<List<EnterpriseGuideTagResponse>> activeTags() {
        return ApiResponse.ok(tagService.listActive(currentTenantId()));
    }

    /** 新增导游标签。 */
    @OperationLog(module = "企业资料", type = "新增")
    @PostMapping("/tags/create")
    public ApiResponse<EnterpriseGuideTagResponse> createTag(
            @Valid @RequestBody EnterpriseGuideTagSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(tagService.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改导游标签。 */
    @OperationLog(module = "企业资料", type = "修改")
    @PostMapping("/tags/update")
    public ApiResponse<EnterpriseGuideTagResponse> updateTag(
            @RequestParam Long id,
            @Valid @RequestBody EnterpriseGuideTagSaveRequest request
    ) {
        return ApiResponse.ok(tagService.update(id, request, currentTenantId()));
    }

    /** 软删除导游标签。 */
    @OperationLog(module = "企业资料", type = "删除")
    @PostMapping("/tags/delete")
    public ApiResponse<Void> deleteTag(@RequestParam Long id, Authentication authentication) {
        tagService.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }
}
