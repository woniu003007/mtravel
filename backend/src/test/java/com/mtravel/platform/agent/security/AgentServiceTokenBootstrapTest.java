package com.mtravel.platform.agent.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.agent.security.config.AgentServiceTokenBootstrapProperties;
import com.mtravel.platform.agent.security.entity.AgentServiceTokenEntity;
import com.mtravel.platform.agent.security.mapper.AgentServiceTokenMapper;
import com.mtravel.platform.agent.security.service.AgentServiceTokenBootstrap;
import com.mtravel.platform.agent.security.service.AgentServiceTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Agent 服务令牌环境变量启动配置测试。 */
class AgentServiceTokenBootstrapTest {

    @Test
    void missingEnvironmentTokenShouldNotWriteDatabase() {
        AgentServiceTokenMapper mapper = mock(AgentServiceTokenMapper.class);
        AgentServiceTokenBootstrapProperties properties = new AgentServiceTokenBootstrapProperties();

        new AgentServiceTokenBootstrap(mapper, properties).provision();

        verify(mapper, never()).insert(any(AgentServiceTokenEntity.class));
    }

    @Test
    void configuredTokenShouldOnlyPersistHashAndPrefix() {
        AgentServiceTokenMapper mapper = mock(AgentServiceTokenMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
        AgentServiceTokenBootstrapProperties properties = new AgentServiceTokenBootstrapProperties();
        properties.setRawToken("agent-development-token-32-characters-minimum");
        properties.setTenantId(3L);
        properties.setTokenName("customer-service-agent");
        properties.setScopes("agent:read:product,agent:write:handoff");

        new AgentServiceTokenBootstrap(mapper, properties).provision();

        ArgumentCaptor<AgentServiceTokenEntity> captor = ArgumentCaptor.forClass(AgentServiceTokenEntity.class);
        verify(mapper).insert(captor.capture());
        AgentServiceTokenEntity entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(3L);
        assertThat(entity.getTokenHash()).isEqualTo(AgentServiceTokenService.hashToken(properties.getRawToken()));
        assertThat(entity.getTokenHash()).doesNotContain(properties.getRawToken());
        assertThat(entity.getTokenPrefix()).isEqualTo("agent-de");
    }
}
