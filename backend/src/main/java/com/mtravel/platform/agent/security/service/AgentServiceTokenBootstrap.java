package com.mtravel.platform.agent.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.agent.security.config.AgentServiceTokenBootstrapProperties;
import com.mtravel.platform.agent.security.entity.AgentServiceTokenEntity;
import com.mtravel.platform.agent.security.mapper.AgentServiceTokenMapper;
import java.time.OffsetDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 根据环境变量幂等配置 Agent 服务令牌。
 *
 * <p>落库时只保存 SHA-256 哈希和短前缀，不保存、记录或返回明文令牌。</p>
 */
@Component
public class AgentServiceTokenBootstrap implements ApplicationRunner {

    private final AgentServiceTokenMapper mapper;
    private final AgentServiceTokenBootstrapProperties properties;

    public AgentServiceTokenBootstrap(
            AgentServiceTokenMapper mapper,
            AgentServiceTokenBootstrapProperties properties
    ) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        provision();
    }

    /** 无环境变量时不做任何写入；有配置时按租户和令牌名称幂等更新。 */
    public void provision() {
        String rawToken = properties.getRawToken();
        if (!StringUtils.hasText(rawToken)) return;
        if (rawToken.length() < 32) {
            throw new IllegalStateException("AGENT_SERVICE_TOKEN 必须至少 32 个字符");
        }
        if (properties.getTenantId() == null || properties.getTenantId() <= 0) {
            throw new IllegalStateException("配置 AGENT_SERVICE_TOKEN 时必须同时配置正数 AGENT_SERVICE_TOKEN_TENANT_ID");
        }
        String tokenName = StringUtils.hasText(properties.getTokenName())
                ? properties.getTokenName().trim()
                : "customer-service-agent";
        if (!StringUtils.hasText(properties.getScopes())) {
            throw new IllegalStateException("AGENT_SERVICE_TOKEN_SCOPES 不能为空");
        }
        OffsetDateTime now = OffsetDateTime.now();
        AgentServiceTokenEntity entity = mapper.selectOne(new QueryWrapper<AgentServiceTokenEntity>()
                .eq("tenant_id", properties.getTenantId())
                .eq("token_name", tokenName)
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        boolean creating = entity == null;
        if (creating) {
            entity = new AgentServiceTokenEntity();
            entity.setTenantId(properties.getTenantId());
            entity.setTokenName(tokenName);
            entity.setCreatedBy("environment_bootstrap");
            entity.setCreatedAt(now);
            entity.setIsDeleted(false);
        }
        entity.setTokenPrefix(rawToken.substring(0, Math.min(8, rawToken.length())));
        entity.setTokenHash(AgentServiceTokenService.hashToken(rawToken));
        entity.setScopes(properties.getScopes().trim());
        entity.setStatus("active");
        entity.setExpiresAt(properties.getExpiresAt());
        entity.setUpdatedAt(now);
        if (creating) mapper.insert(entity); else mapper.updateById(entity);
    }
}
