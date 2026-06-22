package com.mtravel.platform.enterprise.employee.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.department.entity.EnterpriseDepartmentEntity;
import com.mtravel.platform.enterprise.department.mapper.EnterpriseDepartmentMapper;
import com.mtravel.platform.enterprise.employee.dto.EnterpriseEmployeeSaveRequest;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRoleEntity;
import com.mtravel.platform.enterprise.role.mapper.EnterpriseRoleMapper;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseEmployeeServiceTest {

    @Test
    void createShouldCreateEmployeeAndLoginAccountWithDefaultPassword() {
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EnterpriseEmployeeService service = service(employeeMapper, userMapper, passwordEncoder);
        EnterpriseRoleEntity role = role();
        EnterpriseDepartmentEntity department = department();
        ArgumentCaptor<SystemUserEntity> userCaptor = ArgumentCaptor.forClass(SystemUserEntity.class);
        ArgumentCaptor<EnterpriseEmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EnterpriseEmployeeEntity.class);

        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(serviceRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(serviceDepartmentMapper.selectOne(any(Wrapper.class))).thenReturn(department);
        when(passwordEncoder.encode("123456")).thenReturn("hash-123456");
        when(userMapper.insert(any(SystemUserEntity.class))).thenAnswer(invocation -> {
            SystemUserEntity user = invocation.getArgument(0);
            user.setId(21L);
            return 1;
        });
        when(employeeMapper.insert(any(EnterpriseEmployeeEntity.class))).thenAnswer(invocation -> {
            EnterpriseEmployeeEntity employee = invocation.getArgument(0);
            employee.setId(31L);
            return 1;
        });
        when(employeeMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> employeeCaptor.getValue());

        service.create(request(), 1L, "admin");

        verify(userMapper).insert(userCaptor.capture());
        verify(employeeMapper).insert(employeeCaptor.capture());
        SystemUserEntity user = userCaptor.getValue();
        EnterpriseEmployeeEntity employee = employeeCaptor.getValue();
        assertThat(user.getUsername()).isEqualTo("chenaiwan");
        assertThat(user.getPasswordHash()).isEqualTo("hash-123456");
        assertThat(user.getRealName()).isEqualTo("陈爱晚");
        assertThat(user.getRoleCode()).isEqualTo("dispatch");
        assertThat(user.getRoleId()).isEqualTo(9L);
        assertThat(user.getStatus()).isEqualTo("active");
        assertThat(employee.getSystemUserId()).isEqualTo(21L);
        assertThat(employee.getEmployeeCode()).isEqualTo("OP001");
        assertThat(employee.getEmployeeName()).isEqualTo("陈爱晚");
        assertThat(employee.getDepartmentId()).isEqualTo(5L);
        assertThat(employee.getRoleId()).isEqualTo(9L);
        assertThat(employee.getInfoScope()).isEqualTo("department");
        assertThat(employee.getProfitScope()).isEqualTo("personal");
        assertThat(employee.getReceptionScope()).isEqualTo("department");
        assertThat(employee.getCustomerScope()).isEqualTo("company");
    }

    @Test
    void createShouldRejectDuplicateUsername() {
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        EnterpriseEmployeeService service = service(employeeMapper, userMapper, mock(PasswordEncoder.class));

        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request(), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("登录账号已存在");

        verify(userMapper, never()).insert(any(SystemUserEntity.class));
    }

    @Test
    void disableShouldDisableEmployeeAndLoginAccount() {
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        EnterpriseEmployeeService service = service(employeeMapper, userMapper, mock(PasswordEncoder.class));
        EnterpriseEmployeeEntity employee = employee();
        ArgumentCaptor<EnterpriseEmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EnterpriseEmployeeEntity.class);
        ArgumentCaptor<SystemUserEntity> userCaptor = ArgumentCaptor.forClass(SystemUserEntity.class);

        when(employeeMapper.selectOne(any(Wrapper.class))).thenReturn(employee);
        when(employeeMapper.update(any(EnterpriseEmployeeEntity.class), any(UpdateWrapper.class))).thenReturn(1);
        when(userMapper.update(any(SystemUserEntity.class), any(Wrapper.class))).thenReturn(1);

        service.disable(31L, 1L);

        verify(employeeMapper).update(employeeCaptor.capture(), any(UpdateWrapper.class));
        verify(userMapper).update(userCaptor.capture(), any(Wrapper.class));
        assertThat(employeeCaptor.getValue().getStatus()).isEqualTo("disabled");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("disabled");
    }

    @Test
    void deleteShouldSoftDeleteEmployeeAndLoginAccount() {
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        EnterpriseEmployeeService service = service(employeeMapper, userMapper, mock(PasswordEncoder.class));
        EnterpriseEmployeeEntity employee = employee();
        ArgumentCaptor<EnterpriseEmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EnterpriseEmployeeEntity.class);
        ArgumentCaptor<SystemUserEntity> userCaptor = ArgumentCaptor.forClass(SystemUserEntity.class);

        when(employeeMapper.selectOne(any(Wrapper.class))).thenReturn(employee);
        when(employeeMapper.update(any(EnterpriseEmployeeEntity.class), any(UpdateWrapper.class))).thenReturn(1);
        when(userMapper.update(any(SystemUserEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(31L, 1L, "admin");

        verify(employeeMapper).update(employeeCaptor.capture(), any(UpdateWrapper.class));
        verify(userMapper).update(userCaptor.capture(), any(Wrapper.class));
        assertThat(employeeCaptor.getValue().getIsDeleted()).isTrue();
        assertThat(employeeCaptor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(employeeCaptor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(userCaptor.getValue().getIsDeleted()).isTrue();
        assertThat(userCaptor.getValue().getDeletedBy()).isEqualTo("admin");
    }

    @Test
    void resetPasswordShouldWriteDefaultPasswordHash() {
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EnterpriseEmployeeService service = service(employeeMapper, userMapper, passwordEncoder);
        ArgumentCaptor<SystemUserEntity> userCaptor = ArgumentCaptor.forClass(SystemUserEntity.class);

        when(employeeMapper.selectOne(any(Wrapper.class))).thenReturn(employee());
        when(passwordEncoder.encode("123456")).thenReturn("reset-hash");
        when(userMapper.update(any(SystemUserEntity.class), any(Wrapper.class))).thenReturn(1);

        service.resetPassword(31L, 1L);

        verify(userMapper).update(userCaptor.capture(), any(Wrapper.class));
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("reset-hash");
    }

    @Test
    void listAllShouldBatchLoadDepartmentAndRoleNames() {
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        EnterpriseEmployeeService service = service(employeeMapper, mock(SystemUserMapper.class), mock(PasswordEncoder.class));
        EnterpriseEmployeeEntity first = employee(31L, 5L, 9L, "陈爱晚");
        EnterpriseEmployeeEntity second = employee(32L, 5L, 9L, "王计调");

        when(employeeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(first, second));
        when(serviceDepartmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(department()));
        when(serviceRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role()));

        List<?> employees = service.listAll(1L, false);

        assertThat(employees).hasSize(2);
        verify(serviceDepartmentMapper, times(1)).selectList(any(Wrapper.class));
        verify(serviceRoleMapper, times(1)).selectList(any(Wrapper.class));
        verify(serviceDepartmentMapper, never()).selectOne(any(Wrapper.class));
        verify(serviceRoleMapper, never()).selectOne(any(Wrapper.class));
    }

    private EnterpriseRoleMapper serviceRoleMapper;
    private EnterpriseDepartmentMapper serviceDepartmentMapper;

    private EnterpriseEmployeeService service(
            EnterpriseEmployeeMapper employeeMapper,
            SystemUserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        serviceRoleMapper = mock(EnterpriseRoleMapper.class);
        serviceDepartmentMapper = mock(EnterpriseDepartmentMapper.class);
        return new EnterpriseEmployeeService(
                employeeMapper,
                userMapper,
                serviceRoleMapper,
                serviceDepartmentMapper,
                passwordEncoder
        );
    }

    private EnterpriseEmployeeSaveRequest request() {
        return new EnterpriseEmployeeSaveRequest(
                "OP001",
                "陈爱晚",
                "chenaiwan",
                5L,
                9L,
                "female",
                "025-88888888",
                "13800000000",
                "chen@example.com",
                "department",
                "personal",
                "department",
                "company",
                10,
                "active",
                "计调人员"
        );
    }

    private EnterpriseRoleEntity role() {
        EnterpriseRoleEntity role = new EnterpriseRoleEntity();
        role.setId(9L);
        role.setTenantId(1L);
        role.setRoleCode("dispatch");
        role.setRoleName("计调");
        role.setStatus("active");
        role.setIsDeleted(false);
        return role;
    }

    private EnterpriseDepartmentEntity department() {
        EnterpriseDepartmentEntity department = new EnterpriseDepartmentEntity();
        department.setId(5L);
        department.setTenantId(1L);
        department.setDepartmentName("计调部");
        department.setStatus("active");
        department.setIsDeleted(false);
        return department;
    }

    private EnterpriseEmployeeEntity employee() {
        EnterpriseEmployeeEntity employee = new EnterpriseEmployeeEntity();
        employee.setId(31L);
        employee.setTenantId(1L);
        employee.setSystemUserId(21L);
        employee.setEmployeeName("陈爱晚");
        employee.setUsername("chenaiwan");
        employee.setStatus("active");
        employee.setIsDeleted(false);
        return employee;
    }

    private EnterpriseEmployeeEntity employee(Long id, Long departmentId, Long roleId, String employeeName) {
        EnterpriseEmployeeEntity employee = employee();
        employee.setId(id);
        employee.setDepartmentId(departmentId);
        employee.setRoleId(roleId);
        employee.setEmployeeName(employeeName);
        employee.setUsername("user" + id);
        return employee;
    }
}
