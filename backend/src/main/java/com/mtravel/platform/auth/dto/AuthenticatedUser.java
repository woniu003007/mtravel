package com.mtravel.platform.auth.dto;

import java.util.List;

/**
 * 当前已认证用户上下文。
 *
 * <p>该对象来自 JWT 解析结果，业务接口通过它获取用户、租户、角色和当前 Redis 会话 ID。</p>
 *
 * @param id 系统用户 ID
 * @param username 登录账号
 * @param realName 用户姓名
 * @param tenantId 租户 ID
 * @param roles 基础角色编码列表
 * @param sessionId 当前 Redis 在线会话 ID
 */
public record AuthenticatedUser(
        Long id,
        String username,
        String realName,
        Long tenantId,
        List<String> roles,
        String sessionId
) {

    /**
     * 创建不带 sessionId 的用户上下文，主要用于登录成功后生成新会话之前。
     */
    public AuthenticatedUser(Long id, String username, String realName, Long tenantId, List<String> roles) {
        this(id, username, realName, tenantId, roles, null);
    }
}
