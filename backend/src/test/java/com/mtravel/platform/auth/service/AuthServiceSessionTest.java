package com.mtravel.platform.auth.service;

import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.auth.dto.LoginRequest;
import com.mtravel.platform.auth.dto.LoginResult;
import com.mtravel.platform.tenant.TenantProperties;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceSessionTest {

    @Test
    void loginShouldReturnIdleTimeoutAndCreateSessionBeforeToken() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setDemoUsername("demo01");
        securityProperties.setDemoPassword("pwd");
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setDefaultTenantId(1L);
        JwtService jwtService = mock(JwtService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        AuthConfigService authConfigService = mock(AuthConfigService.class);
        SystemUserMapper systemUserMapper = mock(SystemUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SystemUserEntity userEntity = new SystemUserEntity();
        userEntity.setId(1L);
        userEntity.setTenantId(1L);
        userEntity.setUsername("demo01");
        userEntity.setPasswordHash("hash-value");
        userEntity.setRealName("系统管理员");
        userEntity.setRoleCode("admin");
        userEntity.setStatus("active");
        userEntity.setIsDeleted(false);
        when(systemUserMapper.selectOne(any())).thenReturn(userEntity);
        when(passwordEncoder.matches("pwd", "hash-value")).thenReturn(true);
        when(authConfigService.getIdleTimeout(1L)).thenReturn(Duration.ofMinutes(90));
        when(authSessionService.createSession(any(AuthenticatedUser.class), any(Duration.class))).thenReturn("sid-1");
        when(jwtService.createAccessToken(any(AuthenticatedUser.class), org.mockito.Mockito.eq("sid-1")))
                .thenReturn("token-value");
        AuthService service = new AuthService(
                securityProperties,
                tenantProperties,
                jwtService,
                tokenBlacklistService,
                authSessionService,
                authConfigService,
                systemUserMapper,
                passwordEncoder
        );

        LoginResult result = service.login(new LoginRequest("demo01", "pwd"));

        assertThat(result.accessToken()).isEqualTo("token-value");
        assertThat(result.idleTimeoutMinutes()).isEqualTo(90);
        verify(authSessionService).createSession(
                new AuthenticatedUser(1L, "demo01", "系统管理员", 1L, List.of("admin")),
                Duration.ofMinutes(90)
        );
    }
}
