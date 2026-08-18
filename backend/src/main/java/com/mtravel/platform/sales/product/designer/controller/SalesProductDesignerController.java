package com.mtravel.platform.sales.product.designer.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceDeleteRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceReorderRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDetailResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerIntroductionSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerMapResourceResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerResourceDetailResponse;
import com.mtravel.platform.sales.product.designer.service.SalesProductDesignerService;
import com.mtravel.platform.system.log.web.OperationLog;
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
 * 销售产品设计工作台接口。
 *
 * <p>接口以单条资源动作为粒度，服务地图拖拽式编排、资源介绍快照和成人报价草稿，不复用
 * 产品模板完整保存接口，避免页面频繁保存时重建无关子表。</p>
 */
@Validated
@RestController
@RequestMapping("/sales/product/designer")
public class SalesProductDesignerController extends ControllerSupport {

    private final SalesProductDesignerService service;

    public SalesProductDesignerController(
            SalesProductDesignerService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询产品设计草稿，不返回正式产品。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/draft/page")
    public ApiResponse<PageResult<ProductDesignerDraftResponse>> draftPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.pageDrafts(
                currentTenantId(), keyword, businessType, city, page, pageSize
        ));
    }

    /** 查询产品设计草稿基础信息。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/draft/detail")
    public ApiResponse<ProductDesignerDraftResponse> draftDetail(@RequestParam Long id) {
        return ApiResponse.ok(service.draftDetail(currentTenantId(), id));
    }

    /** 新建产品设计草稿，保存后仍不进入产品管理。 */
    @OperationLog(module = "销售管理", type = "新增")
    @PostMapping("/draft/create")
    public ApiResponse<ProductDesignerDraftResponse> createDraft(
            @Valid @RequestBody ProductDesignerDraftSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createDraft(
                currentTenantId(), request, currentOperator(authentication)
        ));
    }

    /** 修改产品设计草稿基础信息。 */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/draft/update")
    public ApiResponse<ProductDesignerDraftResponse> updateDraft(
            @RequestParam Long id,
            @Valid @RequestBody ProductDesignerDraftSaveRequest request
    ) {
        return ApiResponse.ok(service.updateDraft(currentTenantId(), id, request));
    }

    /** 完成产品设计，将草稿转为产品管理中的正式产品。 */
    @OperationLog(module = "销售管理", type = "完成设计")
    @PostMapping("/draft/publish")
    public ApiResponse<Long> publishDraft(@RequestParam Long id) {
        return ApiResponse.ok(service.publishDraft(currentTenantId(), id));
    }

    /** 删除尚未完成设计的产品草稿。 */
    @OperationLog(module = "销售管理", type = "删除")
    @PostMapping("/draft/delete")
    public ApiResponse<Void> deleteDraft(@RequestParam Long id, Authentication authentication) {
        service.deleteDraft(currentTenantId(), id, currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 查询产品设计工作台详情。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<ProductDesignerDetailResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(currentTenantId(), id));
    }

    /** 分页查询地图资源池。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/resources")
    public ApiResponse<PageResult<ProductDesignerMapResourceResponse>> resources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String scenicLevel,
            @RequestParam(required = false) String starLevel,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.resources(
                currentTenantId(),
                keyword,
                resourceType,
                province,
                city,
                scenicLevel,
                starLevel,
                page,
                pageSize
        ));
    }

    /** 查询点选资源详情，包含介绍版本、图片和供应商报价。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/resource-detail")
    public ApiResponse<ProductDesignerResourceDetailResponse> resourceDetail(@RequestParam Long resourceId) {
        return ApiResponse.ok(service.resourceDetail(currentTenantId(), resourceId));
    }

    /** 新增或修改产品某天的一条资源。 */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/day-resource/save")
    public ApiResponse<ProductDesignerDayResourceResponse> saveDayResource(
            @Valid @RequestBody ProductDesignerDayResourceSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.saveDayResource(currentTenantId(), request, currentOperator(authentication)));
    }

    /** 软删除产品某天的一条资源。 */
    @OperationLog(module = "销售管理", type = "删除")
    @PostMapping("/day-resource/delete")
    public ApiResponse<Void> deleteDayResource(
            @Valid @RequestBody ProductDesignerDayResourceDeleteRequest request,
            Authentication authentication
    ) {
        service.deleteDayResource(currentTenantId(), request, currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 保存产品当天资源顺序。 */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/day-resource/reorder")
    public ApiResponse<Void> reorderDayResources(
            @Valid @RequestBody ProductDesignerDayResourceReorderRequest request
    ) {
        service.reorderDayResources(currentTenantId(), request);
        return ApiResponse.ok();
    }

    /** 单独保存产品某条资源的介绍版本选择。 */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/day-resource/intro")
    public ApiResponse<ProductDesignerDayResourceResponse> saveIntroduction(
            @Valid @RequestBody ProductDesignerIntroductionSaveRequest request
    ) {
        return ApiResponse.ok(service.saveIntroduction(currentTenantId(), request));
    }

    /** 保存产品成人报价草稿。 */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/adult-quote/save")
    public ApiResponse<ProductDesignerAdultQuoteResponse> saveAdultQuote(
            @Valid @RequestBody ProductDesignerAdultQuoteSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.saveAdultQuote(currentTenantId(), request, currentOperator(authentication)));
    }
}
