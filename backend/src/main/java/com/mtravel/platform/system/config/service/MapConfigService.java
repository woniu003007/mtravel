package com.mtravel.platform.system.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.system.config.dto.MapConfigResponse;
import com.mtravel.platform.system.config.dto.MapConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 高德地图配置服务。
 *
 * <p>地图相关 Key 保存在租户级系统配置中。查询接口只返回脱敏值；
 * 产品路书和地图代理服务需要真实 Key 时从 system_configs 读取原值。</p>
 */
@Service
public class MapConfigService {

    /** 高德 Web 服务 Key，用于后端代理输入提示、路线规划和静态图。 */
    public static final String AMAP_WEB_SERVICE_KEY = "map.amap.web_service_key";
    /** 高德 JS API Key，用于浏览器交互地图。 */
    public static final String AMAP_JS_KEY = "map.amap.js_key";
    /** 高德 JS API 安全密钥。 */
    public static final String AMAP_JS_SECURITY_CODE = "map.amap.js_security_code";

    private final SystemConfigMapper mapper;

    public MapConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 查询高德地图配置，所有 Key 只返回脱敏值。 */
    public MapConfigResponse getMapConfig(Long tenantId) {
        return new MapConfigResponse(
                mask(configValue(tenantId, AMAP_WEB_SERVICE_KEY)),
                mask(configValue(tenantId, AMAP_JS_KEY)),
                mask(configValue(tenantId, AMAP_JS_SECURITY_CODE))
        );
    }

    /** 保存高德地图配置。空字段不覆盖已保存 Key。 */
    public MapConfigResponse updateMapConfig(Long tenantId, MapConfigUpdateRequest request) {
        upsertIfPresent(tenantId, AMAP_WEB_SERVICE_KEY, request.webServiceKey(), "高德地图Web服务Key");
        upsertIfPresent(tenantId, AMAP_JS_KEY, request.jsKey(), "高德地图JS API Key");
        upsertIfPresent(tenantId, AMAP_JS_SECURITY_CODE, request.jsSecurityCode(), "高德地图JS API安全密钥");
        return new MapConfigResponse(
                mask(StringUtils.hasText(request.webServiceKey()) ? request.webServiceKey() : configValue(tenantId, AMAP_WEB_SERVICE_KEY)),
                mask(StringUtils.hasText(request.jsKey()) ? request.jsKey() : configValue(tenantId, AMAP_JS_KEY)),
                mask(StringUtils.hasText(request.jsSecurityCode()) ? request.jsSecurityCode() : configValue(tenantId, AMAP_JS_SECURITY_CODE))
        );
    }

    private String configValue(Long tenantId, String key) {
        SystemConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, key));
        return entity == null ? "" : entity.getConfigValue();
    }

    private void upsertIfPresent(Long tenantId, String key, String value, String remark) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setTenantId(tenantId);
        entity.setConfigKey(key);
        entity.setConfigValue(value.trim());
        entity.setRemark(remark);
        mapper.upsert(entity);
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() <= 8) {
            return "********";
        }
        return cleaned.substring(0, 4) + "********" + cleaned.substring(cleaned.length() - 4);
    }
}
