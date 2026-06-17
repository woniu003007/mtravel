package com.mtravel.platform.sales.product.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateRequest;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateResponse;
import com.mtravel.platform.sales.product.dto.AmapJsConfigResponse;
import com.mtravel.platform.sales.product.dto.AmapStaticMapRequest;
import com.mtravel.platform.sales.product.dto.AmapTipResponse;
import com.mtravel.platform.sales.product.dto.SalesProductResponse;
import com.mtravel.platform.sales.product.dto.SalesProductSaveRequest;
import com.mtravel.platform.sales.product.service.AmapRouteService;
import com.mtravel.platform.sales.product.service.SalesProductService;
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
 * 销售产品模板管理接口。
 *
 * <p>Controller 只负责接收页面参数、解析当前租户和操作人。产品名称唯一、行程天数、
 * 子表保存等业务规则全部放在 SalesProductService。</p>
 */
@Validated
@RestController
@RequestMapping("/sales/product")
public class SalesProductController extends ControllerSupport {

    private final SalesProductService service;
    private final AmapRouteService amapRouteService;

    public SalesProductController(
            SalesProductService service,
            AmapRouteService amapRouteService,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
        this.amapRouteService = amapRouteService;
    }

    /**
     * 分页查询产品模板。
     *
     * @return 产品分页结果
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<SalesProductResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String receptionStandard,
            @RequestParam(required = false) String domesticInternational,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.page(
                currentTenantId(),
                keyword,
                businessType,
                receptionStandard,
                domesticInternational,
                status,
                page,
                pageSize
        ));
    }

    /**
     * 查询产品详情。
     *
     * @param id 产品ID
     * @return 产品详情，包含行程、说明和团队安排参数
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/detail")
    public ApiResponse<SalesProductResponse> detail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /**
     * 新增产品模板。
     *
     * @param request 产品保存请求
     * @param authentication 当前登录认证信息，用于记录创建人
     * @return 新增后的产品详情
     */
    @OperationLog(module = "销售管理", type = "新增")
    @PostMapping("/create")
    public ApiResponse<SalesProductResponse> create(
            @Valid @RequestBody SalesProductSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.create(request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 修改产品模板。
     *
     * @param id 产品ID
     * @param request 产品保存请求
     * @param authentication 当前登录认证信息，用于记录子表重建操作人
     * @return 修改后的产品详情
     */
    @OperationLog(module = "销售管理", type = "修改")
    @PostMapping("/update")
    public ApiResponse<SalesProductResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody SalesProductSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.update(id, request, currentTenantId(), currentOperator(authentication)));
    }

    /**
     * 软删除产品模板。
     *
     * @param id 产品ID
     * @param authentication 当前登录认证信息，用于记录删除人
     * @return 空响应
     */
    @OperationLog(module = "销售管理", type = "删除")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /**
     * 搜索高德地图地点候选。
     *
     * @param keywords 地点关键字
     * @param city 城市，可为空
     * @return 地点候选列表
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/roadbook/amap/tips")
    public ApiResponse<List<AmapTipResponse>> amapTips(
            @RequestParam String keywords,
            @RequestParam(required = false) String city
    ) {
        return ApiResponse.ok(amapRouteService.searchTips(currentTenantId(), keywords, city));
    }

    /**
     * 获取高德 JavaScript 地图前端加载配置。
     *
     * @return JS API Key 和安全密钥配置
     */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/roadbook/amap/js-config")
    public ApiResponse<AmapJsConfigResponse> amapJsConfig() {
        return ApiResponse.ok(amapRouteService.jsConfig(currentTenantId()));
    }

    /**
     * 计算高德驾车路线。
     *
     * @param request 路线点位
     * @return 路线总距离、总车程和分段信息
     */
    @OperationLog(module = "销售管理", type = "查询")
    @PostMapping("/roadbook/amap/driving")
    public ApiResponse<AmapRouteCalculateResponse> amapDriving(
            @Valid @RequestBody AmapRouteCalculateRequest request
    ) {
        return ApiResponse.ok(amapRouteService.calculateDrivingRoute(currentTenantId(), request));
    }

    /**
     * 生成高德静态地图预览地址。
     *
     * <p>用于产品路书抽屉展示路线地图。前端不直接保存高德 Web 服务 Key。</p>
     *
     * @param request 路线点位
     * @return 静态地图 base64 图片
     */
    @OperationLog(module = "销售管理", type = "查询")
    @PostMapping("/roadbook/amap/static-map")
    public ApiResponse<String> amapStaticMap(
            @Valid @RequestBody AmapStaticMapRequest request
    ) {
        return ApiResponse.ok(amapRouteService.staticMapImage(currentTenantId(), request));
    }
}
