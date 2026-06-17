package com.mtravel.platform.system.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.system.config.dto.AuthConfigResponse;
import com.mtravel.platform.system.config.dto.AuthConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class AuthConfigService {

    public static final String LOGIN_IDLE_TIMEOUT_MINUTES = "login_idle_timeout_minutes";

    private final SecurityProperties securityProperties;
    private final SystemConfigMapper mapper;

    public AuthConfigService(SecurityProperties securityProperties, SystemConfigMapper mapper) {
        this.securityProperties = securityProperties;
        this.mapper = mapper;
    }

    public Duration getIdleTimeout(Long tenantId) {
        SystemConfigEntity entity = mapper.selectOne(baseQuery(tenantId, LOGIN_IDLE_TIMEOUT_MINUTES));
        if (entity == null) {
            return Duration.ofMinutes(securityProperties.getIdleTimeoutMinutes());
        }
        try {
            return Duration.ofMinutes(Long.parseLong(entity.getConfigValue()));
        } catch (NumberFormatException ex) {
            return Duration.ofMinutes(securityProperties.getIdleTimeoutMinutes());
        }
    }

    public AuthConfigResponse getAuthConfig(Long tenantId) {
        return new AuthConfigResponse(getIdleTimeout(tenantId).toMinutes());
    }

    public AuthConfigResponse updateAuthConfig(Long tenantId, AuthConfigUpdateRequest request) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setTenantId(tenantId);
        entity.setConfigKey(LOGIN_IDLE_TIMEOUT_MINUTES);
        entity.setConfigValue(String.valueOf(request.idleTimeoutMinutes()));
        entity.setRemark("浏览器无操作自动退出时间，单位分钟");
        mapper.upsert(entity);
        return getAuthConfig(tenantId);
    }

    private LambdaQueryWrapper<SystemConfigEntity> baseQuery(Long tenantId, String key) {
        return new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, key);
    }
}
