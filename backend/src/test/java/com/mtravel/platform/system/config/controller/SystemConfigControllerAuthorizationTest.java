package com.mtravel.platform.system.config.controller;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.system.config.dto.AuthConfigResponse;
import com.mtravel.platform.system.config.dto.AuthConfigUpdateRequest;
import com.mtravel.platform.system.config.dto.BusinessRiskConfigResponse;
import com.mtravel.platform.system.config.dto.BusinessRiskConfigUpdateRequest;
import com.mtravel.platform.system.config.service.AiConfigService;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.config.service.BusinessRiskConfigService;
import com.mtravel.platform.system.config.service.MapConfigService;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import com.mtravel.platform.tenant.TenantProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 客户授信审批开关权限回归测试。 */
class SystemConfigControllerAuthorizationTest {

    @Test
    void businessRiskUpdateShouldRejectOrdinaryEmployee() {
        BusinessRiskConfigService businessRiskService = mock(BusinessRiskConfigService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        SystemConfigController controller = controller(businessRiskService, mock(AuthConfigService.class), userMapper);
        when(userMapper.selectOne(any())).thenReturn(databaseUser("sales", false));

        assertThatThrownBy(() -> controller.updateBusinessRiskConfig(
                new BusinessRiskConfigUpdateRequest(true), authentication(9L, 1L, "sales")))
                .isInstanceOf(AccessDeniedException.class);

        verify(businessRiskService, never()).updateBusinessRiskConfig(any(), any());
    }

    @Test
    void businessRiskUpdateShouldAllowDatabaseAdminRole() {
        BusinessRiskConfigService businessRiskService = mock(BusinessRiskConfigService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        SystemConfigController controller = controller(businessRiskService, mock(AuthConfigService.class), userMapper);
        BusinessRiskConfigUpdateRequest request = new BusinessRiskConfigUpdateRequest(true);
        when(userMapper.selectOne(any())).thenReturn(databaseUser("admin", false));
        when(businessRiskService.updateBusinessRiskConfig(1L, request))
                .thenReturn(new BusinessRiskConfigResponse(true));

        assertThat(controller.updateBusinessRiskConfig(request, authentication(9L, 1L, "sales"))
                .data().customerRiskApprovalEnabled()).isTrue();
        verify(businessRiskService).updateBusinessRiskConfig(1L, request);
    }

    @Test
    void businessRiskUpdateShouldAllowTenantAdministrator() {
        BusinessRiskConfigService businessRiskService = mock(BusinessRiskConfigService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        SystemConfigController controller = controller(businessRiskService, mock(AuthConfigService.class), userMapper);
        BusinessRiskConfigUpdateRequest request = new BusinessRiskConfigUpdateRequest(true);
        when(userMapper.selectOne(any())).thenReturn(databaseUser("sales", true));

        controller.updateBusinessRiskConfig(request, authentication(9L, 1L, "sales"));

        verify(businessRiskService).updateBusinessRiskConfig(1L, request);
    }

    @Test
    void businessRiskReadAndOtherConfigUpdateShouldRemainUnrestricted() {
        BusinessRiskConfigService businessRiskService = mock(BusinessRiskConfigService.class);
        AuthConfigService authConfigService = mock(AuthConfigService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        SystemConfigController controller = controller(businessRiskService, authConfigService, userMapper);
        when(businessRiskService.getBusinessRiskConfig(1L)).thenReturn(new BusinessRiskConfigResponse(false));
        when(authConfigService.updateAuthConfig(1L, new AuthConfigUpdateRequest(30)))
                .thenReturn(new AuthConfigResponse(30));

        assertThat(controller.businessRiskConfig().data().customerRiskApprovalEnabled()).isFalse();
        assertThat(controller.updateAuthConfig(new AuthConfigUpdateRequest(30)).data().idleTimeoutMinutes())
                .isEqualTo(30);
        verify(userMapper, never()).selectOne(any());
    }

    private SystemConfigController controller(
            BusinessRiskConfigService businessRiskService,
            AuthConfigService authConfigService,
            SystemUserMapper userMapper
    ) {
        TenantProperties properties = new TenantProperties();
        properties.setDefaultTenantId(1L);
        return new SystemConfigController(
                authConfigService,
                mock(AiConfigService.class),
                businessRiskService,
                mock(MapConfigService.class),
                properties,
                userMapper
        );
    }

    private Authentication authentication(Long userId, Long tenantId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "operator", "测试用户", tenantId, List.of(role));
        return new UsernamePasswordAuthenticationToken(user, "token");
    }

    private SystemUserEntity databaseUser(String role, boolean tenantAdmin) {
        SystemUserEntity user = new SystemUserEntity();
        user.setId(9L);
        user.setTenantId(1L);
        user.setUsername("operator");
        user.setRoleCode(role);
        user.setIsTenantAdmin(tenantAdmin);
        user.setStatus("active");
        user.setIsDeleted(false);
        return user;
    }
}
