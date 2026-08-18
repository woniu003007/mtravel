package com.mtravel.platform.common.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import com.mtravel.platform.system.config.service.MapConfigService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 高德 Web 服务公共客户端。
 *
 * <p>统一处理租户级 Key、环境变量回退、请求节流和高德错误响应，避免采购、销售等业务模块
 * 分别维护一套第三方调用规则。Web 服务 Key 只在后端拼装请求，不返回给浏览器。</p>
 */
@Component
public class AmapWebServiceClient {

    private static final long MIN_REQUEST_INTERVAL_MILLIS = 250L;

    private final RestTemplate restTemplate;
    private final SystemConfigMapper configMapper;
    private final String envWebServiceKey;
    private final Object amapRateLimitLock = new Object();
    private long lastAmapRequestAt;

    public AmapWebServiceClient(
            RestTemplateBuilder restTemplateBuilder,
            SystemConfigMapper configMapper,
            @Value("${AMAP_WEB_SERVICE_KEY:}") String envWebServiceKey
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.configMapper = configMapper;
        this.envWebServiceKey = envWebServiceKey;
    }

    /**
     * 调用高德 JSON 接口并校验标准 status 响应。
     *
     * @param tenantId 当前租户 ID
     * @param endpoint 高德接口完整地址
     * @param queryParams 业务查询参数，不包含 Key
     * @return 高德响应对象
     */
    public Map<?, ?> getJson(Long tenantId, String endpoint, Map<String, ?> queryParams) {
        String url = requestUrl(tenantId, endpoint, queryParams);
        try {
            waitForAmapRequestSlot();
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            Map<?, ?> result = response == null ? Map.of() : response;
            ensureAmapSuccess(result);
            return result;
        } catch (RestClientException ex) {
            throw new BizException("高德地图服务暂时不可用");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("高德地图请求被中断");
        }
    }

    /**
     * 调用高德图片接口，供销售路书静态地图继续复用公共 Key 和节流规则。
     *
     * @param tenantId 当前租户 ID
     * @param endpoint 高德接口完整地址
     * @param queryParams 业务查询参数，不包含 Key
     * @return 图片字节
     */
    public byte[] getBytes(Long tenantId, String endpoint, Map<String, ?> queryParams) {
        String url = requestUrl(tenantId, endpoint, queryParams);
        try {
            waitForAmapRequestSlot();
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] image = response.getBody();
            if (image == null || image.length == 0) {
                throw new BizException("高德地图预览为空");
            }
            return image;
        } catch (RestClientException ex) {
            throw new BizException("高德地图预览暂时不可用");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("高德地图请求被中断");
        }
    }

    /** 读取租户级地图配置原值，仅供后端地图服务内部使用。 */
    public String configValue(Long tenantId, String key) {
        SystemConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, key));
        return entity == null ? null : entity.getConfigValue();
    }

    /** 从租户配置读取 Web 服务 Key，读取不到时回退到环境变量。 */
    public String webServiceKey(Long tenantId) {
        String key = configValue(tenantId, MapConfigService.AMAP_WEB_SERVICE_KEY);
        if (!StringUtils.hasText(key)) {
            key = envWebServiceKey;
        }
        if (!StringUtils.hasText(key)) {
            throw new BizException("未配置高德地图 Web服务 Key");
        }
        return key.trim();
    }

    private String requestUrl(Long tenantId, String endpoint, Map<String, ?> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("key", webServiceKey(tenantId));
        queryParams.forEach((name, value) -> {
            if (value != null) {
                builder.queryParam(name, value);
            }
        });
        return builder.build().toUriString();
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
}
