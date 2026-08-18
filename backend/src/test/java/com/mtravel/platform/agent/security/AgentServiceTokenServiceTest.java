package com.mtravel.platform.agent.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.security.entity.AgentServiceTokenEntity;
import com.mtravel.platform.agent.security.mapper.AgentServiceTokenMapper;
import com.mtravel.platform.agent.security.service.AgentServiceTokenService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agent 服务令牌认证测试。 */
class AgentServiceTokenServiceTest {

    @Test
    void authenticateShouldReturnTenantBoundScopesForActiveToken() {
        AgentServiceTokenMapper mapper = mock(AgentServiceTokenMapper.class);
        AgentServiceTokenEntity entity = token("active", OffsetDateTime.now().plusDays(1));
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(entity);
        AgentServiceTokenService service = new AgentServiceTokenService(mapper);

        var principal = service.authenticate("agent-test-secret");

        assertThat(principal.tokenId()).isEqualTo(9L);
        assertThat(principal.tenantId()).isEqualTo(3L);
        assertThat(principal.scopes()).containsExactlyInAnyOrder(
                "agent:read:product",
                "agent:write:handoff"
        );
    }

    @Test
    void authenticateShouldRejectExpiredToken() {
        AgentServiceTokenMapper mapper = mock(AgentServiceTokenMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(token("active", OffsetDateTime.now().minusMinutes(1)));
        AgentServiceTokenService service = new AgentServiceTokenService(mapper);

        assertThatThrownBy(() -> service.authenticate("agent-test-secret"))
                .isInstanceOf(AgentException.class)
                .hasMessage("Agent 服务令牌无效或已过期");
    }

    private AgentServiceTokenEntity token(String status, OffsetDateTime expiresAt) {
        AgentServiceTokenEntity entity = new AgentServiceTokenEntity();
        entity.setId(9L);
        entity.setTenantId(3L);
        entity.setTokenName("客服 Agent");
        entity.setTokenHash(AgentServiceTokenService.hashToken("agent-test-secret"));
        entity.setScopes("agent:read:product, agent:write:handoff");
        entity.setStatus(status);
        entity.setExpiresAt(expiresAt);
        entity.setIsDeleted(false);
        return entity;
    }
}
