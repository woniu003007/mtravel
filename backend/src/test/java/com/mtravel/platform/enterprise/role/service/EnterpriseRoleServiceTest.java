package com.mtravel.platform.enterprise.role.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRolePermissionSaveRequest;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRoleSaveRequest;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRoleEntity;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRolePermissionEntity;
import com.mtravel.platform.enterprise.role.mapper.EnterpriseRoleMapper;
import com.mtravel.platform.enterprise.role.mapper.EnterpriseRolePermissionMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseRoleServiceTest {

    @Test
    void createShouldRejectDuplicateRoleCode() {
        EnterpriseRoleMapper roleMapper = mock(EnterpriseRoleMapper.class);
        EnterpriseRoleService service = service(roleMapper);
        EnterpriseRoleSaveRequest request = request("dispatch", "计调");

        when(roleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色编码已存在");

        verify(roleMapper, never()).insert(any(EnterpriseRoleEntity.class));
    }

    @Test
    void createShouldPersistRoleFields() {
        EnterpriseRoleMapper roleMapper = mock(EnterpriseRoleMapper.class);
        EnterpriseRoleService service = service(roleMapper);
        EnterpriseRoleSaveRequest request = request("dispatch_manager", "计调经理");
        ArgumentCaptor<EnterpriseRoleEntity> captor = ArgumentCaptor.forClass(EnterpriseRoleEntity.class);

        when(roleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        final EnterpriseRoleEntity[] insertedRole = new EnterpriseRoleEntity[1];
        when(roleMapper.insert(any(EnterpriseRoleEntity.class))).thenAnswer(invocation -> {
            EnterpriseRoleEntity entity = invocation.getArgument(0);
            entity.setId(8L);
            insertedRole[0] = entity;
            return 1;
        });
        when(roleMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedRole[0]);

        service.create(request, 1L, "admin");

        verify(roleMapper).insert(captor.capture());
        EnterpriseRoleEntity entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getRoleCode()).isEqualTo("dispatch_manager");
        assertThat(entity.getRoleName()).isEqualTo("计调经理");
        assertThat(entity.getSortOrder()).isEqualTo(20);
        assertThat(entity.getSystemBuiltin()).isFalse();
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void deleteShouldRejectRoleUsedByEmployees() {
        EnterpriseRoleMapper roleMapper = mock(EnterpriseRoleMapper.class);
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        EnterpriseRoleService service = service(roleMapper, employeeMapper, mock(EnterpriseRolePermissionMapper.class));

        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(9L, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色已被员工使用");

        verify(roleMapper, never()).update(any(EnterpriseRoleEntity.class), any(Wrapper.class));
    }

    @Test
    void savePermissionsShouldReplaceRolePermissions() {
        EnterpriseRoleMapper roleMapper = mock(EnterpriseRoleMapper.class);
        EnterpriseRolePermissionMapper permissionMapper = mock(EnterpriseRolePermissionMapper.class);
        EnterpriseRoleService service = service(roleMapper, mock(EnterpriseEmployeeMapper.class), permissionMapper);
        EnterpriseRoleEntity role = new EnterpriseRoleEntity();
        role.setId(9L);
        role.setTenantId(1L);
        role.setRoleCode("finance");
        role.setRoleName("财务");
        role.setStatus("active");
        role.setIsDeleted(false);
        EnterpriseRolePermissionSaveRequest request = new EnterpriseRolePermissionSaveRequest(List.of(
                new EnterpriseRolePermissionSaveRequest.PermissionItem(
                        "finance",
                        "财务管理",
                        "finance.audit",
                        "财务审核",
                        "menu",
                        10
                )
        ));

        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(permissionMapper.update(any(), any())).thenReturn(1);
        when(permissionMapper.insert(any(EnterpriseRolePermissionEntity.class))).thenReturn(1);

        service.savePermissions(9L, request, 1L, "admin");

        verify(permissionMapper).update(any(), any());
        verify(permissionMapper).insert(any(EnterpriseRolePermissionEntity.class));
    }

    private EnterpriseRoleService service(EnterpriseRoleMapper roleMapper) {
        return service(roleMapper, mock(EnterpriseEmployeeMapper.class), mock(EnterpriseRolePermissionMapper.class));
    }

    private EnterpriseRoleService service(
            EnterpriseRoleMapper roleMapper,
            EnterpriseEmployeeMapper employeeMapper,
            EnterpriseRolePermissionMapper permissionMapper
    ) {
        return new EnterpriseRoleService(roleMapper, permissionMapper, employeeMapper);
    }

    private EnterpriseRoleSaveRequest request(String code, String name) {
        return new EnterpriseRoleSaveRequest(code, name, 20, "active", "角色说明");
    }
}
