package com.mtravel.platform.auth.service;

import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.auth.dto.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * JWT 访问令牌服务。
 *
 * <p>Token 中保存用户基础信息、租户 ID、基础角色和 Redis sessionId。
 * 业务接口只信任签名校验通过且 Redis 会话仍有效的 token。</p>
 */
@Service
public class JwtService {

    private final SecurityProperties properties;
    private final SecretKey key;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据用户上下文创建访问令牌。
     *
     * @param user 当前已认证用户
     * @return 可放入 Authorization Bearer 的 JWT
     */
    public String createAccessToken(AuthenticatedUser user) {
        return createAccessToken(user, user.sessionId());
    }

    /**
     * 根据用户上下文和登录会话 ID 创建访问令牌。
     *
     * @param user 当前已认证用户
     * @param sessionId Redis 在线会话 ID，用于后续单账号在线和无操作超时校验
     * @return 可放入 Authorization Bearer 的 JWT
     */
    public String createAccessToken(AuthenticatedUser user, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .claim("userId", user.id())
                .claim("realName", user.realName())
                .claim("tenantId", user.tenantId())
                .claim("roles", user.roles())
                .claim("sessionId", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenMinutes() * 60)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 JWT 签名。
     *
     * @param token 前端传入的 Bearer token
     * @return token 中携带的用户上下文
     */
    @SuppressWarnings("unchecked")
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long userId = claims.get("userId", Number.class).longValue();
        Long tenantId = claims.get("tenantId", Number.class).longValue();
        String realName = claims.get("realName", String.class);
        String sessionId = claims.get("sessionId", String.class);
        List<String> roles = (List<String>) claims.get("roles", List.class);
        return new AuthenticatedUser(userId, claims.getSubject(), realName, tenantId, roles, sessionId);
    }
}
