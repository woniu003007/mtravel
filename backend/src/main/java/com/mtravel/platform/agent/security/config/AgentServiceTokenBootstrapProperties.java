package com.mtravel.platform.agent.security.config;

import java.time.OffsetDateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Agent 服务令牌的环境变量启动配置，不向接口暴露明文。 */
@ConfigurationProperties(prefix = "mtravel.agent.service-token")
public class AgentServiceTokenBootstrapProperties {

    private String rawToken;
    private Long tenantId;
    private String tokenName = "customer-service-agent";
    private String scopes = String.join(",",
            "agent:read:customer-context",
            "agent:read:product",
            "agent:read:schedule",
            "agent:read:policy",
            "agent:read:quote-request",
            "agent:write:quote-request",
            "agent:write:handoff"
    );
    private OffsetDateTime expiresAt;

    public String getRawToken() { return rawToken; }
    public void setRawToken(String rawToken) { this.rawToken = rawToken; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getTokenName() { return tokenName; }
    public void setTokenName(String tokenName) { this.tokenName = tokenName; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
