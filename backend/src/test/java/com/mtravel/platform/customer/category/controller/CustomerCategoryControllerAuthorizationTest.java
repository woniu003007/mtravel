package com.mtravel.platform.customer.category.controller;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.dto.CustomerCategoryCreateRequest;
import com.mtravel.platform.customer.category.dto.CustomerCategoryUpdateRequest;
import com.mtravel.platform.customer.category.service.CustomerCategoryService;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import com.mtravel.platform.tenant.TenantProperties;
import java.math.BigDecimal;
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

/** 客户等级授信配置接口权限回归测试。 */
class CustomerCategoryControllerAuthorizationTest {

    @Test
    void writeOperationsShouldRejectOrdinaryEmployee() {
        CustomerCategoryService service = mock(CustomerCategoryService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        CustomerCategoryController controller = controller(service, userMapper);
        when(userMapper.selectOne(any())).thenReturn(databaseUser("sales", false));
        Authentication authentication = authentication(9L, 1L, "sales");

        assertThatThrownBy(() -> controller.create(createRequest(), authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.update(3L, updateRequest(), authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.delete(3L, authentication))
                .isInstanceOf(AccessDeniedException.class);

        verify(service, never()).create(any(), any(), any());
        verify(service, never()).update(any(), any(), any(), any());
        verify(service, never()).delete(any(), any(), any());
    }

    @Test
    void writeOperationsShouldAllowDatabaseAdminRole() {
        CustomerCategoryService service = mock(CustomerCategoryService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        CustomerCategoryController controller = controller(service, userMapper);
        when(userMapper.selectOne(any())).thenReturn(databaseUser("admin", false));
        Authentication authentication = authentication(9L, 1L, "sales");

        controller.create(createRequest(), authentication);
        controller.update(3L, updateRequest(), authentication);
        controller.delete(3L, authentication);

        verify(service).create(createRequest(), 1L, "operator");
        verify(service).update(3L, updateRequest(), 1L, "operator");
        verify(service).delete(3L, 1L, "operator");
    }

    @Test
    void writeOperationsShouldAllowTenantAdministrator() {
        CustomerCategoryService service = mock(CustomerCategoryService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        CustomerCategoryController controller = controller(service, userMapper);
        when(userMapper.selectOne(any())).thenReturn(databaseUser("sales", true));

        controller.create(createRequest(), authentication(9L, 1L, "sales"));

        verify(service).create(createRequest(), 1L, "operator");
    }

    @Test
    void readOperationsShouldRemainAvailableWithoutAdministratorCheck() {
        CustomerCategoryService service = mock(CustomerCategoryService.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        CustomerCategoryController controller = controller(service, userMapper);
        when(service.page(1L, null, null, 1, 20)).thenReturn(new PageResult<>(List.of(), 0));

        assertThat(controller.page(null, null, 1, 20).data().total()).isZero();
        verify(userMapper, never()).selectOne(any());
    }

    private CustomerCategoryController controller(CustomerCategoryService service, SystemUserMapper userMapper) {
        TenantProperties properties = new TenantProperties();
        properties.setDefaultTenantId(1L);
        return new CustomerCategoryController(service, properties, userMapper);
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

    private CustomerCategoryCreateRequest createRequest() {
        return new CustomerCategoryCreateRequest(
                "金牌客户", new BigDecimal("10000.00"), 30, true, List.of(), List.of(), 10, "active", null
        );
    }

    private CustomerCategoryUpdateRequest updateRequest() {
        return new CustomerCategoryUpdateRequest(
                "金牌客户", new BigDecimal("10000.00"), 30, true, List.of(), List.of(), 10, "active", null
        );
    }
}
