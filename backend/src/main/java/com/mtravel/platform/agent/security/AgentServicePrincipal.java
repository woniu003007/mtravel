package com.mtravel.platform.agent.security;

import java.util.Set;

/** 已认证的 Agent 服务调用方。 */
public record AgentServicePrincipal(
        Long tokenId,
        Long tenantId,
        String tokenName,
        Set<String> scopes
) {
    /** 判断服务令牌是否具备指定 Scope。 */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
}
