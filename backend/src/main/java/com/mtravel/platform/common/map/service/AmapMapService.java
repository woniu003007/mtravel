package com.mtravel.platform.common.map.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.map.dto.AmapJsConfigResponse;
import com.mtravel.platform.common.map.dto.AmapRegeoResponse;
import com.mtravel.platform.common.map.dto.AmapTipResponse;
import com.mtravel.platform.system.config.service.MapConfigService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 公共高德地点服务。
 *
 * <p>为采购资源、销售产品等模块提供统一的地点搜索、JS 地图配置和逆地理编码能力。
 * 查询结果按租户缓存，避免短时间重复消耗高德接口额度。</p>
 */
@Service
public class AmapMapService {

    private static final String INPUT_TIPS_ENDPOINT = "https://restapi.amap.com/v3/assistant/inputtips";
    private static final String REGEO_ENDPOINT = "https://restapi.amap.com/v3/geocode/regeo";
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");

    private final AmapWebServiceClient client;
    private final Map<String, CacheEntry<List<AmapTipResponse>>> tipsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<AmapRegeoResponse>> regeoCache = new ConcurrentHashMap<>();

    public AmapMapService(AmapWebServiceClient client) {
        this.client = client;
    }

    /**
     * 按关键字搜索高德地点候选。
     *
     * @param tenantId 当前租户 ID
     * @param keywords 地点关键字，空值直接返回空列表
     * @param city 城市筛选，可为空
     * @return 最多 20 个有效地点候选
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
        Map<?, ?> response = client.getJson(tenantId, INPUT_TIPS_ENDPOINT, Map.of(
                "keywords", normalizedKeywords,
                "city", normalizedCity,
                "citylimit", false
        ));
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
        tipsCache.put(cacheKey, cacheEntry(result));
        return result;
    }

    /**
     * 查询浏览器加载高德 JavaScript API 所需配置。
     *
     * <p>优先使用独立 JS Key；未配置时维持原有行为，回退到 Web 服务 Key。</p>
     */
    public AmapJsConfigResponse jsConfig(Long tenantId) {
        String jsKey = client.configValue(tenantId, MapConfigService.AMAP_JS_KEY);
        if (!StringUtils.hasText(jsKey)) {
            jsKey = client.webServiceKey(tenantId);
        }
        String securityCode = client.configValue(tenantId, MapConfigService.AMAP_JS_SECURITY_CODE);
        return new AmapJsConfigResponse(
                jsKey.trim(),
                StringUtils.hasText(securityCode) ? securityCode.trim() : null
        );
    }

    /**
     * 根据经纬度获取格式化详细地址。
     *
     * @param tenantId 当前租户 ID
     * @param longitude GCJ-02 经度，范围 -180 至 180
     * @param latitude GCJ-02 纬度，范围 -90 至 90
     * @return 格式化详细地址
     */
    public AmapRegeoResponse reverseGeocode(Long tenantId, BigDecimal longitude, BigDecimal latitude) {
        validateCoordinates(longitude, latitude);
        String normalizedLongitude = longitude.stripTrailingZeros().toPlainString();
        String normalizedLatitude = latitude.stripTrailingZeros().toPlainString();
        String cacheKey = "regeo:%s:%s:%s".formatted(tenantId, normalizedLongitude, normalizedLatitude);
        AmapRegeoResponse cached = cachedValue(regeoCache, cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<?, ?> response = client.getJson(tenantId, REGEO_ENDPOINT, Map.of(
                "location", normalizedLongitude + "," + normalizedLatitude,
                "extensions", "all",
                "radius", 1000
        ));
        Map<?, ?> regeocode = mapValue(response.get("regeocode"));
        String address = stringValue(regeocode.get("formatted_address"));
        if (!StringUtils.hasText(address)) {
            throw new BizException("高德未返回可用地址");
        }
        Map<?, ?> addressComponent = mapValue(regeocode.get("addressComponent"));
        String province = stringValue(addressComponent.get("province"));
        String city = firstStringValue(addressComponent.get("city"));
        if (!StringUtils.hasText(city)) {
            city = province;
        }
        String district = stringValue(addressComponent.get("district"));
        AmapRegeoResponse result = new AmapRegeoResponse(address, province, city, district);
        regeoCache.put(cacheKey, cacheEntry(result));
        return result;
    }

    private void validateCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new BizException("经纬度不能为空");
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new BizException("经度必须在-180到180之间");
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new BizException("纬度必须在-90到90之间");
        }
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

    private <T> CacheEntry<T> cacheEntry(T value) {
        return new CacheEntry<>(value, System.currentTimeMillis() + CACHE_TTL_MILLIS);
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

    private Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) && !"[]".equals(text) ? text : null;
    }

    private String firstStringValue(Object value) {
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(this::stringValue)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        }
        return stringValue(value);
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }
}
