package com.mtravel.platform.agent.security.service;

import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Agent Controller 的认证主体和 Scope 校验服务。 */
@Service
public class AgentAccessService {

    /** 取得 Agent 服务主体并校验最小 Scope。 */
    public AgentServicePrincipal require(Authentication authentication, String scope) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AgentServicePrincipal principal)) {
            throw AgentException.unauthorized("缺少有效的 Agent 服务令牌");
        }
        if (!principal.hasScope(scope)) {
            throw AgentException.forbidden("Agent 服务令牌缺少权限：" + scope);
        }
        return principal;
    }
}
