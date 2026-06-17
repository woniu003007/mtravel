package com.mtravel.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.auth.dto.LoginRequest;
import com.mtravel.platform.auth.dto.LoginResult;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import com.mtravel.platform.tenant.TenantProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录认证服务。
 *
 * <p>当前阶段只做账号登录、登出、JWT 签发和 Redis 会话有效性控制；完整菜单权限和按钮权限后续再接角色权限表。</p>
 */
@Service
public class AuthService {

    private final SecurityProperties securityProperties;
    private final TenantProperties tenantProperties;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthSessionService authSessionService;
    private final AuthConfigService authConfigService;
    private final SystemUserMapper systemUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            SecurityProperties securityProperties,
            TenantProperties tenantProperties,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            AuthSessionService authSessionService,
            AuthConfigService authConfigService,
            SystemUserMapper systemUserMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.securityProperties = securityProperties;
        this.tenantProperties = tenantProperties;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.authSessionService = authSessionService;
        this.authConfigService = authConfigService;
        this.systemUserMapper = systemUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 使用数据库账号完成登录。
     *
     * <p>登录规则：按当前默认租户和用户名查询未删除账号，只允许启用账号登录，密码使用 BCrypt 哈希比对。
     * 登录成功后写入 Redis 会话，再把 sessionId 放进 JWT，后续请求用它校验会话是否已退出或无操作超时。
     * 系统开发阶段允许同一账号多处登录，单账号在线限制等系统开发完成后再接入。</p>
     *
     * @param request 登录账号和密码
     * @return 登录用户信息、访问令牌和当前租户配置的无操作超时时间
     */
    public LoginResult login(LoginRequest request) {
        Long tenantId = tenantProperties.getDefaultTenantId();
        SystemUserEntity userEntity = systemUserMapper.selectOne(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getTenantId, tenantId)
                .eq(SystemUserEntity::getUsername, request.username())
                .eq(SystemUserEntity::getIsDeleted, false));
        // 只允许未删除且启用的账号登录，密码只做哈希校验，不做明文比对。
        if (userEntity == null
                || !"active".equals(userEntity.getStatus())
                || !passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        AuthenticatedUser user = new AuthenticatedUser(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getRealName(),
                userEntity.getTenantId(),
                List.of(userEntity.getRoleCode())
        );
        Duration idleTimeout = authConfigService.getIdleTimeout(user.tenantId());
        String sessionId = authSessionService.createSession(user, idleTimeout);
        return new LoginResult(user.id(), user.username(), user.realName(), user.roles(), "/workspace",
                jwtService.createAccessToken(user, sessionId), idleTimeout.toMinutes());
    }

    /**
     * 退出登录。
     *
     * <p>退出时删除 Redis 在线会话，并把当前 JWT 加入黑名单，避免退出后的旧 token 继续访问接口。</p>
     *
     * @param token 前端 Authorization 里解析出来的 Bearer token
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        AuthenticatedUser user = jwtService.parse(token);
        authSessionService.logout(user, user.sessionId());
        tokenBlacklistService.blacklist(token, Duration.ofMinutes(securityProperties.getAccessTokenMinutes()));
    }
}
