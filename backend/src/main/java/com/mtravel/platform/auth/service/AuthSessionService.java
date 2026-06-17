package com.mtravel.platform.auth.service;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 登录在线会话服务。
 *
 * <p>当前开发阶段允许同一账号在多个浏览器或设备同时登录，Redis 主要保存
 * “sessionId -> 用户信息”，用于无操作自动退出和请求心跳续期。
 * “用户 -> 当前 sessionId”映射仅作为最近登录记录保留，不参与拦截。</p>
 */
@Service
public class AuthSessionService {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String USER_SESSION_KEY_PREFIX = "auth:user-session:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public AuthSessionService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 创建登录会话。
     *
     * <p>系统开发阶段先不启用同账号单点在线控制。即使 Redis 中已有该用户的登录记录，
     * 也继续创建新的 session，避免开发和测试时频繁被旧浏览器会话挡住。
     * 系统上线前如需恢复单账号限制，可在这里改为显式强制登录或踢下线流程。</p>
     *
     * @param user 登录成功后的用户上下文
     * @param ttl 会话有效期，来自租户系统配置
     * @return 新生成的 sessionId，后续会写入 JWT
     */
    public String createSession(AuthenticatedUser user, Duration ttl) {
        StringRedisTemplate redisTemplate = requiredRedisTemplate();
        String userSessionKey = userSessionKey(user);
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(sessionKey(sessionId), sessionPayload(user), ttl);
        redisTemplate.opsForValue().set(userSessionKey, sessionId, ttl);
        return sessionId;
    }

    /**
     * 校验当前请求携带的 sessionId 是否仍有效，并刷新无操作超时时间。
     *
     * @param user JWT 中解析出的用户上下文
     * @param sessionId JWT 中保存的 sessionId
     * @param ttl 当前租户配置的无操作超时时长
     * @return true 表示会话有效；false 表示已退出或超时
     */
    public boolean validateAndRefresh(AuthenticatedUser user, String sessionId, Duration ttl) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }
        try {
            StringRedisTemplate redisTemplate = requiredRedisTemplate();
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)))) {
                return false;
            }
            redisTemplate.expire(sessionKey(sessionId), ttl);
            redisTemplate.expire(userSessionKey(user), ttl);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 删除当前登录会话。
     *
     * <p>退出登录时删除当前 sessionId 键。用户当前会话映射只是最近登录记录，
     * 如果它正好指向本次退出的 session，则同步删除；如果用户已经在别处重新登录，
     * 则保留新的最近登录记录。</p>
     *
     * @param user 当前登录用户
     * @param sessionId 当前 token 中的 sessionId
     */
    public void logout(AuthenticatedUser user, String sessionId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        if (StringUtils.hasText(sessionId)) {
            redisTemplate.delete(sessionKey(sessionId));
        }
        String userSessionKey = userSessionKey(user);
        String currentSessionId = redisTemplate.opsForValue().get(userSessionKey);
        if (sessionId != null && sessionId.equals(currentSessionId)) {
            redisTemplate.delete(userSessionKey);
        }
    }

    private StringRedisTemplate requiredRedisTemplate() {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            throw new IllegalStateException("Redis 未配置，无法建立登录会话");
        }
        return redisTemplate;
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionKey(AuthenticatedUser user) {
        return USER_SESSION_KEY_PREFIX + user.tenantId() + ":" + user.id();
    }

    private String sessionPayload(AuthenticatedUser user) {
        return "tenantId=%s;userId=%s;username=%s;loginAt=%s".formatted(
                user.tenantId(),
                user.id(),
                user.username(),
                OffsetDateTime.now()
        );
    }
}
