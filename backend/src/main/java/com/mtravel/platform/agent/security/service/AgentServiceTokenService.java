package com.mtravel.platform.agent.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.entity.AgentServiceTokenEntity;
import com.mtravel.platform.agent.security.mapper.AgentServiceTokenMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Agent 服务令牌认证服务。 */
@Service
public class AgentServiceTokenService {

    private final AgentServiceTokenMapper mapper;

    public AgentServiceTokenService(AgentServiceTokenMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 校验服务令牌并返回令牌绑定的租户和 Scope。
     *
     * @param rawToken Bearer 令牌明文，仅在当前调用栈内使用
     * @return 已认证服务调用方
     */
    public AgentServicePrincipal authenticate(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw AgentException.unauthorized("缺少 Agent 服务令牌");
        }
        AgentServiceTokenEntity entity = mapper.selectOne(new QueryWrapper<AgentServiceTokenEntity>()
                .eq("token_hash", hashToken(rawToken))
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (entity == null
                || !"active".equals(entity.getStatus())
                || (entity.getExpiresAt() != null && !entity.getExpiresAt().isAfter(now))) {
            throw AgentException.unauthorized("Agent 服务令牌无效或已过期");
        }
        mapper.update(null, new UpdateWrapper<AgentServiceTokenEntity>()
                .eq("id", entity.getId())
                .eq("tenant_id", entity.getTenantId())
                .set("last_used_at", now));
        return new AgentServicePrincipal(
                entity.getId(),
                entity.getTenantId(),
                entity.getTokenName(),
                parseScopes(entity.getScopes())
        );
    }

    /** 对高熵服务令牌计算不可逆 SHA-256 哈希。 */
    public static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", exception);
        }
    }

    private Set<String> parseScopes(String scopes) {
        if (!StringUtils.hasText(scopes)) {
            return Set.of();
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }
}
