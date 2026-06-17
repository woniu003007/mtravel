package com.mtravel.platform.common;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.tenant.TenantContextHolder;
import com.mtravel.platform.tenant.TenantProperties;
import org.springframework.security.core.Authentication;

/**
 * 控制器通用上下文工具。
 *
 * <p>业务 Controller 经常需要获取当前租户和操作人。集中封装后，新增模块不会反复复制
 * TenantContextHolder 和 Authentication 解析逻辑，也能减少后续多租户入口调整的改动面。</p>
 */
public class ControllerSupport {

    private final TenantProperties tenantProperties;

    public ControllerSupport(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * 获取当前租户 ID。未登录或未传租户头时使用默认租户。
     */
    public Long currentTenantId() {
        return TenantContextHolder.getTenantId(tenantProperties.getDefaultTenantId());
    }

    /**
     * 获取当前操作人账号。认证上下文不存在时返回 system，主要用于初始化或测试请求。
     */
    public String currentOperator(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.username();
        }
        return "system";
    }
}
