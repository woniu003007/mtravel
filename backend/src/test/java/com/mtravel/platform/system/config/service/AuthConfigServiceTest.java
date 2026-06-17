package com.mtravel.platform.system.config.service;

import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.system.config.dto.AuthConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthConfigServiceTest {

    @Test
    void getIdleTimeoutShouldReadTenantConfigWhenExists() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setIdleTimeoutMinutes(120);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigValue("90");
        when(mapper.selectOne(any())).thenReturn(entity);
        AuthConfigService service = new AuthConfigService(securityProperties, mapper);

        assertThat(service.getIdleTimeout(1L)).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void getIdleTimeoutShouldFallbackToDefaultWhenConfigMissing() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setIdleTimeoutMinutes(120);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        AuthConfigService service = new AuthConfigService(securityProperties, mapper);

        assertThat(service.getIdleTimeout(1L)).isEqualTo(Duration.ofMinutes(120));
    }

    @Test
    void updateAuthConfigShouldPersistIdleTimeoutMinutes() {
        SecurityProperties securityProperties = new SecurityProperties();
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        AuthConfigService service = new AuthConfigService(securityProperties, mapper);

        service.updateAuthConfig(1L, new AuthConfigUpdateRequest(30));

        ArgumentCaptor<SystemConfigEntity> captor = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(mapper).upsert(captor.capture());
        SystemConfigEntity entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getConfigKey()).isEqualTo(AuthConfigService.LOGIN_IDLE_TIMEOUT_MINUTES);
        assertThat(entity.getConfigValue()).isEqualTo("30");
    }
}
