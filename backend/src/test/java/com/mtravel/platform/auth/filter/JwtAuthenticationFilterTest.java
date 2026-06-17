package com.mtravel.platform.auth.filter;

import com.mtravel.platform.auth.service.AuthSessionService;
import com.mtravel.platform.auth.service.JwtService;
import com.mtravel.platform.auth.service.TokenBlacklistService;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.log.service.OperationLogService;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void blacklistedTokenShouldReturnUnauthorized() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        TenantProperties tenantProperties = new TenantProperties();
        OperationLogService operationLogService = mock(OperationLogService.class);
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        AuthConfigService authConfigService = mock(AuthConfigService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                tokenBlacklistService,
                tenantProperties,
                operationLogService,
                authSessionService,
                authConfigService
        );

        when(tokenBlacklistService.isBlacklisted("old-token")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/codes");
        request.addHeader("Authorization", "Bearer old-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("登录状态已失效");
        verify(jwtService, never()).parse("old-token");
        verify(filterChain, never()).doFilter(request, response);
    }
}
