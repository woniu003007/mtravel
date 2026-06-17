package com.mtravel.platform.sales.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateRequest;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateResponse;
import com.mtravel.platform.sales.product.dto.AmapRoutePointRequest;
import com.mtravel.platform.sales.product.dto.AmapRouteSegmentResponse;
import com.mtravel.platform.sales.product.dto.AmapJsConfigResponse;
import com.mtravel.platform.sales.product.dto.AmapStaticMapRequest;
import com.mtravel.platform.sales.product.dto.AmapTipResponse;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 高德地图 Web 服务代理。
 *
 * <p>前端不直接持有 Web 服务 Key。后端从租户系统配置或环境变量读取 Key，统一代理地点搜索和驾车路线计算。</p>
 */
@Service
public class AmapRouteService {

    public static final String AMAP_WEB_SERVICE_KEY = "map.amap.web_service_key";
    public static final String AMAP_JS_KEY = "map.amap.js_key";
    public static final String AMAP_JS_SECURITY_CODE = "map.amap.js_security_code";
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 250L;

    private final RestTemplate restTemplate;
    private final SystemConfigMapper configMapper;
    private final String envWebServiceKey;
    private final Map<String, CacheEntry<List<AmapTipResponse>>> tipsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<AmapRouteSegmentResponse>> segmentCache = new ConcurrentHashMap<>();
    private final Object amapRateLimitLock = new Object();
    private long lastAmapRequestAt = 0L;

    public AmapRouteService(
            RestTemplateBuilder restTemplateBuilder,
            SystemConfigMapper configMapper,
            @Value("${AMAP_WEB_SERVICE_KEY:}") String envWebServiceKey
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.configMapper = configMapper;
        this.envWebServiceKey = envWebServiceKey;
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
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        String normalizedKeywords = keywords.trim();
        String normalizedCity = city == null ? "" : city.trim();
        String cacheKey = "tips:%s:%s:%s".formatted(tenantId, normalizedCity, normalizedKeywords);
        List<AmapTipResponse> cached = cachedValue(tipsCache, cacheKey);
        if (cached != null) {
            return cached;
        }
        String url = UriComponentsBuilder
                .fromUriString("https://restapi.amap.com/v3/assistant/inputtips")
                .queryParam("key", webServiceKey(tenantId))
                .queryParam("keywords", normalizedKeywords)
                .queryParam("city", normalizedCity)
                .queryParam("citylimit", false)
                .build()
                .toUriString();
        Map<?, ?> response = getForMap(url);
        ensureAmapSuccess(response);
        Object tipsObject = response.get("tips");
        if (!(tipsObject instanceof List<?> tips)) {
            return List.of();
        }
        List<AmapTipResponse> result = tips.stream()
                .filter(Map.class::isInstance)
                .map(item -> tipFromMap((Map<?, ?>) item))
                .filter(item -> StringUtils.hasText(item.name()) && StringUtils.hasText(item.longitude()))
                .limit(20)
                .toList();
        tipsCache.put(cacheKey, new CacheEntry<>(result, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return result;
    }

    /**
     * 查询前端交互地图加载配置。
     *
     * <p>高德 JS API 要在浏览器端加载，因此 Key 会下发给前端；安全密钥按高德 JS API 2.0
     * 要求写入 window._AMapSecurityConfig。正式环境建议单独配置 Web 端 JS API Key。</p>
     */
    public AmapJsConfigResponse jsConfig(Long tenantId) {
        String jsKey = configValue(tenantId, AMAP_JS_KEY);
        if (!StringUtils.hasText(jsKey)) {
            jsKey = webServiceKey(tenantId);
        }
        String securityCode = configValue(tenantId, AMAP_JS_SECURITY_CODE);
        return new AmapJsConfigResponse(jsKey.trim(), securityCode == null ? null : securityCode.trim());
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
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://restapi.amap.com/v3/direction/driving")
                .queryParam("key", webServiceKey(tenantId))
                .queryParam("origin", location(originPoint))
                .queryParam("destination", location(destinationPoint))
                .queryParam("extensions", "base")
                .queryParam("strategy", 0);
        Map<?, ?> response = getForMap(builder.build().toUriString());
        ensureAmapSuccess(response);
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
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://restapi.amap.com/v3/staticmap")
                .queryParam("key", webServiceKey(tenantId))
                .queryParam("size", "700*360")
                .queryParam("scale", 2)
                .queryParam("markers", markerParam(points));
        if (points.size() >= 2) {
            builder.queryParam("paths", pathParam(points));
        }
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(builder.build().toUriString(), byte[].class);
            byte[] image = response.getBody();
            if (image == null || image.length == 0) {
                throw new BizException("高德地图预览为空");
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
        } catch (RestClientException ex) {
            throw new BizException("高德地图预览暂时不可用");
        }
    }

    /** 从系统配置读取 Web 服务 Key，读取不到时回退到环境变量。 */
    private String webServiceKey(Long tenantId) {
        String key = configValue(tenantId, AMAP_WEB_SERVICE_KEY);
        if (!StringUtils.hasText(key)) {
            key = envWebServiceKey;
        }
        if (!StringUtils.hasText(key)) {
            throw new BizException("未配置高德地图 Web服务 Key");
        }
        return key.trim();
    }

    private String configValue(Long tenantId, String key) {
        SystemConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, key));
        return entity == null ? null : entity.getConfigValue();
    }

    private Map<?, ?> getForMap(String url) {
        try {
            waitForAmapRequestSlot();
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            return response == null ? Map.of() : response;
        } catch (RestClientException ex) {
            throw new BizException("高德地图服务暂时不可用");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("高德地图请求被中断");
        }
    }

    private void ensureAmapSuccess(Map<?, ?> response) {
        if (!"1".equals(String.valueOf(response.get("status")))) {
            Object info = response.get("info");
            if ("CUQPS_HAS_EXCEEDED_THE_LIMIT".equals(String.valueOf(info))) {
                throw new BizException("高德地图调用过于频繁，请稍后再试");
            }
            throw new BizException("高德地图调用失败：" + (info == null ? "未知错误" : info));
        }
    }

    private void waitForAmapRequestSlot() throws InterruptedException {
        synchronized (amapRateLimitLock) {
            long now = System.currentTimeMillis();
            long waitMillis = MIN_REQUEST_INTERVAL_MILLIS - (now - lastAmapRequestAt);
            if (waitMillis > 0) {
                Thread.sleep(waitMillis);
            }
            lastAmapRequestAt = System.currentTimeMillis();
        }
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

    private AmapTipResponse tipFromMap(Map<?, ?> item) {
        String location = stringValue(item.get("location"));
        String longitude = null;
        String latitude = null;
        if (StringUtils.hasText(location) && location.contains(",")) {
            String[] parts = location.split(",", 2);
            longitude = parts[0];
            latitude = parts[1];
        }
        return new AmapTipResponse(
                stringValue(item.get("name")),
                stringValue(item.get("address")),
                stringValue(item.get("district")),
                longitude,
                latitude
        );
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) && !"[]".equals(text) ? text : null;
    }
}
