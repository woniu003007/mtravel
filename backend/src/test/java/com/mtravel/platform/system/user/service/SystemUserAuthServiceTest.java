package com.mtravel.platform.system.user.service;

import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.auth.dto.LoginRequest;
import com.mtravel.platform.auth.dto.LoginResult;
import com.mtravel.platform.auth.service.AuthService;
import com.mtravel.platform.auth.service.AuthSessionService;
import com.mtravel.platform.auth.service.JwtService;
import com.mtravel.platform.auth.service.TokenBlacklistService;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import com.mtravel.platform.tenant.TenantProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemUserAuthServiceTest {

    @Test
    void loginShouldUseActiveDatabaseUser() {
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        AuthConfigService authConfigService = mock(AuthConfigService.class);
        AuthService service = authService(userMapper, passwordEncoder, jwtService, tokenBlacklistService,
                authSessionService, authConfigService);
        SystemUserEntity user = systemUser();

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("pwd", "hash-value")).thenReturn(true);
        when(authConfigService.getIdleTimeout(1L)).thenReturn(Duration.ofMinutes(120));
        when(authSessionService.createSession(any(AuthenticatedUser.class), eq(Duration.ofMinutes(120)))).thenReturn("sid-1");
        when(jwtService.createAccessToken(any(AuthenticatedUser.class), eq("sid-1"))).thenReturn("token-value");

        LoginResult result = service.login(new LoginRequest("admin", "pwd"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.realName()).isEqualTo("系统管理员");
        assertThat(result.roles()).containsExactly("admin");
        assertThat(result.accessToken()).isEqualTo("token-value");
        assertThat(result.idleTimeoutMinutes()).isZero();
        verify(authSessionService).createSession(
                new AuthenticatedUser(1L, "admin", "系统管理员", 1L, List.of("admin")),
                Duration.ofMinutes(120)
        );
    }

    @Test
    void loginShouldRejectWhenPasswordNotMatch() {
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService service = authService(userMapper, passwordEncoder, mock(JwtService.class),
                mock(TokenBlacklistService.class), mock(AuthSessionService.class), mock(AuthConfigService.class));

        when(userMapper.selectOne(any())).thenReturn(systemUser());
        when(passwordEncoder.matches("bad", "hash-value")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "bad")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    private AuthService authService(
            SystemUserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            AuthSessionService authSessionService,
            AuthConfigService authConfigService
    ) {
        SecurityProperties securityProperties = new SecurityProperties();
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setDefaultTenantId(1L);
        return new AuthService(
                securityProperties,
                tenantProperties,
                jwtService,
                tokenBlacklistService,
                authSessionService,
                authConfigService,
                userMapper,
                passwordEncoder
        );
    }

    private SystemUserEntity systemUser() {
        SystemUserEntity user = new SystemUserEntity();
        user.setId(1L);
        user.setTenantId(1L);
        user.setUsername("admin");
        user.setPasswordHash("hash-value");
        user.setRealName("系统管理员");
        user.setRoleCode("admin");
        user.setStatus("active");
        user.setIsDeleted(false);
        return user;
    }
}
