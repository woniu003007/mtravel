package com.mtravel.platform.auth.service;

import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * JWT 黑名单服务。
 *
 * <p>用户主动退出后，把当前 token 放入 Redis 黑名单，避免 token 在自然过期前继续访问系统。</p>
 */
@Service
public class TokenBlacklistService {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public TokenBlacklistService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 将 token 加入黑名单。
     *
     * @param token 需要失效的 JWT
     * @param ttl 黑名单保留时长，通常与 token 剩余有效期保持一致
     */
    public void blacklist(String token, Duration ttl) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null || token == null || token.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(token), "1", ttl);
        } catch (RuntimeException ignored) {
            // Redis is an auxiliary dependency in v1; ordinary business APIs must not fail because Redis is unavailable.
        }
    }

    /**
     * 判断 token 是否已经被主动退出失效。
     *
     * @param token 前端传入的 JWT
     * @return true 表示 token 已失效，不能继续访问业务接口
     */
    public boolean isBlacklisted(String token) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null || token == null || token.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String key(String token) {
        return "auth:blacklist:" + token;
    }
}
