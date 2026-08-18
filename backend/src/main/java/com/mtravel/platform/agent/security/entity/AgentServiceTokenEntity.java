package com.mtravel.platform.agent.security.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/** Agent 服务令牌实体，只保存哈希和权限，不保存令牌明文。 */
@TableName("agent_service_tokens")
public class AgentServiceTokenEntity extends TenantSoftDeleteEntity {

    @TableField("token_name")
    private String tokenName;
    @TableField("token_prefix")
    private String tokenPrefix;
    @TableField("token_hash")
    private String tokenHash;
    @TableField("scopes")
    private String scopes;
    @TableField("status")
    private String status;
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
    @TableField("last_used_at")
    private OffsetDateTime lastUsedAt;

    public String getTokenName() { return tokenName; }
    public void setTokenName(String tokenName) { this.tokenName = tokenName; }
    public String getTokenPrefix() { return tokenPrefix; }
    public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
