package com.mtravel.platform.sales.product.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.map.service.AmapMapService;
import com.mtravel.platform.common.map.service.AmapWebServiceClient;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateRequest;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateResponse;
import com.mtravel.platform.sales.product.dto.AmapRoutePointRequest;
import com.mtravel.platform.sales.product.dto.AmapRouteSegmentResponse;
import com.mtravel.platform.sales.product.dto.AmapJsConfigResponse;
import com.mtravel.platform.sales.product.dto.AmapStaticMapRequest;
import com.mtravel.platform.sales.product.dto.AmapTipResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 销售产品高德路线服务。
 *
 * <p>路线计算和静态图属于销售产品业务；地点搜索、JS 配置、租户 Key 和请求节流委托给公共地图服务。
 * 原销售接口继续调用本类，从而保持既有接口路径和返回结构兼容。</p>
 */
@Service
public class AmapRouteService {

    private static final String DRIVING_ENDPOINT = "https://restapi.amap.com/v3/direction/driving";
    private static final String STATIC_MAP_ENDPOINT = "https://restapi.amap.com/v3/staticmap";
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private final AmapMapService mapService;
    private final AmapWebServiceClient amapClient;
    private final Map<String, CacheEntry<AmapRouteSegmentResponse>> segmentCache = new ConcurrentHashMap<>();

    public AmapRouteService(AmapMapService mapService, AmapWebServiceClient amapClient) {
        this.mapService = mapService;
        this.amapClient = amapClient;
    }

    /**
     * 调用高德输入提示接口搜索地点。
     *
     * @param tenantId 当前租户ID
     * @param keywords 地点关键字
     * @param city 城市，可为空
     * @return 地点候选列表
     */
    public List<AmapTipResponse> searchTips(Long tenantId, String keywords, String city) {
        return mapService.searchTips(tenantId, keywords, city).stream()
                .map(item -> new AmapTipResponse(
                        item.name(),
                        item.address(),
                        item.district(),
                        item.longitude(),
                        item.latitude()
                ))
                .toList();
    }

    /**
     * 查询前端交互地图加载配置。
     *
     * <p>高德 JS API 要在浏览器端加载，因此 Key 会下发给前端；安全密钥按高德 JS API 2.0
     * 要求写入 window._AMapSecurityConfig。正式环境建议单独配置 Web 端 JS API Key。</p>
     */
    public AmapJsConfigResponse jsConfig(Long tenantId) {
        var config = mapService.jsConfig(tenantId);
        return new AmapJsConfigResponse(config.key(), config.securityJsCode());
    }

    /**
     * 调用高德驾车路线规划接口计算多点路线。
     *
     * @param tenantId 当前租户ID
     * @param request 路线点位
     * @return 总距离、总时长和分段距离时长
     */
    public AmapRouteCalculateResponse calculateDrivingRoute(Long tenantId, AmapRouteCalculateRequest request) {
        List<AmapRoutePointRequest> points = request.points();
        List<AmapRouteSegmentResponse> segments = new ArrayList<>();
        int totalDistance = 0;
        int totalDuration = 0;
        for (int index = 0; index < points.size() - 1; index += 1) {
            AmapRouteSegmentResponse segment = calculateDrivingSegment(tenantId, points.get(index), points.get(index + 1));
            segments.add(segment);
            totalDistance += segment.distanceMeters();
            totalDuration += segment.durationSeconds();
        }
        return new AmapRouteCalculateResponse(totalDistance, totalDuration, segments);
    }

    /**
     * 计算两个相邻路书点之间的真实驾车距离和车程。
     *
     * <p>这里不能把多点整条路线一次性请求后平均分配，因为前端“到下一站”字段要求的是当前点到下一个点的
     * 精确驾车距离；平均分配会导致每个点显示相同公里数。</p>
     */
    private AmapRouteSegmentResponse calculateDrivingSegment(
            Long tenantId,
            AmapRoutePointRequest originPoint,
            AmapRoutePointRequest destinationPoint
    ) {
        String cacheKey = "driving:%s:%s:%s".formatted(tenantId, location(originPoint), location(destinationPoint));
        AmapRouteSegmentResponse cached = cachedValue(segmentCache, cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<?, ?> response = amapClient.getJson(tenantId, DRIVING_ENDPOINT, Map.of(
                "origin", location(originPoint),
                "destination", location(destinationPoint),
                "extensions", "base",
                "strategy", 0
        ));
        Map<?, ?> route = mapValue(response.get("route"));
        List<?> paths = listValue(route.get("paths"));
        if (paths.isEmpty()) {
            throw new BizException("高德未返回可用路线");
        }
        Map<?, ?> firstPath = mapValue(paths.get(0));
        AmapRouteSegmentResponse result = new AmapRouteSegmentResponse(
                intValue(firstPath.get("distance")),
                intValue(firstPath.get("duration"))
        );
        segmentCache.put(cacheKey, new CacheEntry<>(result, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return result;
    }

    /**
     * 生成高德静态地图图片。
     *
     * <p>前端只传路线点位，后端补 Web 服务 Key 并组装 markers/paths。这样页面可以显示地图预览，
     * 同时不把 Key 暴露给浏览器。</p>
     *
     * @param tenantId 当前租户ID
     * @param request 路线点位
     * @return 可直接用于 img src 的 base64 图片
     */
    public String staticMapImage(Long tenantId, AmapStaticMapRequest request) {
        List<AmapRoutePointRequest> points = request.points();
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("size", "700*360");
        queryParams.put("scale", 2);
        queryParams.put("markers", markerParam(points));
        if (points.size() >= 2) {
            queryParams.put("paths", pathParam(points));
        }
        byte[] image = amapClient.getBytes(tenantId, STATIC_MAP_ENDPOINT, queryParams);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
    }

    private <T> T cachedValue(Map<String, CacheEntry<T>> cache, String key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt() < System.currentTimeMillis()) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }

    private String location(AmapRoutePointRequest point) {
        return point.longitude() + "," + point.latitude();
    }

    private String markerParam(List<AmapRoutePointRequest> points) {
        return "mid,,A:" + String.join(";", points.stream().map(this::location).toList());
    }

    private String pathParam(List<AmapRoutePointRequest> points) {
        return "8,0x1677ff,0.85,,:" + String.join(";", points.stream().map(this::location).toList());
    }

    private Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private List<?> listValue(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}
