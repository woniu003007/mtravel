package com.mtravel.platform.common.map.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.map.dto.AmapJsConfigResponse;
import com.mtravel.platform.common.map.dto.AmapRegeoResponse;
import com.mtravel.platform.common.map.dto.AmapTipResponse;
import com.mtravel.platform.common.map.service.AmapMapService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共高德地图接口。
 *
 * <p>面向采购、销售等业务页面提供统一地点能力，Controller 仅解析租户和请求参数。</p>
 */
@Validated
@RestController
@RequestMapping("/common/map/amap")
public class CommonAmapController extends ControllerSupport {

    private final AmapMapService service;

    public CommonAmapController(AmapMapService service, TenantProperties tenantProperties) {
        super(tenantProperties);
        this.service = service;
    }

    /** 搜索地点候选，空关键字返回空列表。 */
    @OperationLog(module = "公共地图", type = "查询")
    @GetMapping("/tips")
    public ApiResponse<List<AmapTipResponse>> tips(
            @RequestParam String keywords,
            @RequestParam(required = false) String city
    ) {
        return ApiResponse.ok(service.searchTips(currentTenantId(), keywords, city));
    }

    /** 获取高德 JavaScript API 加载配置。 */
    @OperationLog(module = "公共地图", type = "查询")
    @GetMapping("/js-config")
    public ApiResponse<AmapJsConfigResponse> jsConfig() {
        return ApiResponse.ok(service.jsConfig(currentTenantId()));
    }

    /** 根据 GCJ-02 经纬度查询格式化详细地址。 */
    @OperationLog(module = "公共地图", type = "查询")
    @GetMapping("/regeo")
    public ApiResponse<AmapRegeoResponse> reverseGeocode(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude
    ) {
        return ApiResponse.ok(service.reverseGeocode(currentTenantId(), longitude, latitude));
    }
}
