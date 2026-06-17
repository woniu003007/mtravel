package com.mtravel.platform.auth.dto;

import java.util.List;

/**
 * 登录成功响应。
 *
 * @param id 系统用户 ID
 * @param username 登录账号
 * @param realName 用户姓名
 * @param roles 基础角色编码列表
 * @param homePath 登录后默认进入的前端页面
 * @param accessToken 访问令牌，前端后续接口放入 Authorization Bearer
 * @param idleTimeoutMinutes 当前租户配置的无操作退出分钟数
 */
public record LoginResult(
        Long id,
        String username,
        String realName,
        List<String> roles,
        String homePath,
        String accessToken,
        long idleTimeoutMinutes
) {

    /**
     * 兼容旧测试和旧调用的构造方法，默认无操作退出时间为 120 分钟。
     */
    public LoginResult(Long id, String username, String realName, List<String> roles, String homePath, String accessToken) {
        this(id, username, realName, roles, homePath, accessToken, 120);
    }
}
