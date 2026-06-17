package com.mtravel.platform.enterprise.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.department.entity.EnterpriseDepartmentEntity;
import com.mtravel.platform.enterprise.department.mapper.EnterpriseDepartmentMapper;
import com.mtravel.platform.enterprise.employee.dto.EnterpriseEmployeeResponse;
import com.mtravel.platform.enterprise.employee.dto.EnterpriseEmployeeSaveRequest;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.enums.EnterpriseEmployeeGender;
import com.mtravel.platform.enterprise.employee.enums.EnterpriseEmployeeScope;
import com.mtravel.platform.enterprise.employee.enums.EnterpriseEmployeeStatus;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRoleEntity;
import com.mtravel.platform.enterprise.role.mapper.EnterpriseRoleMapper;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 企业员工业务服务。
 *
 * <p>员工管理集中处理员工资料、登录账号、部门角色有效性、四类查看范围、停用和软删除联动。
 * 账号密码只写哈希值，初始和重置密码当前固定为 123456。</p>
 */
@Service
public class EnterpriseEmployeeService extends BusinessCrudService<EnterpriseEmployeeEntity, EnterpriseEmployeeResponse> {

    /** 当前业务确认的员工初始和重置密码。 */
    public static final String DEFAULT_PASSWORD = "123456";

    private final EnterpriseEmployeeMapper employeeMapper;
    private final SystemUserMapper systemUserMapper;
    private final EnterpriseRoleMapper roleMapper;
    private final EnterpriseDepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public EnterpriseEmployeeService(
            EnterpriseEmployeeMapper employeeMapper,
            SystemUserMapper systemUserMapper,
            EnterpriseRoleMapper roleMapper,
            EnterpriseDepartmentMapper departmentMapper,
            PasswordEncoder passwordEncoder
    ) {
        super(employeeMapper);
        this.employeeMapper = employeeMapper;
        this.systemUserMapper = systemUserMapper;
        this.roleMapper = roleMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 分页查询员工。
     *
     * @param tenantId 当前租户 ID
     * @param keyword 员工编码、员工姓名、账号、电话关键字
     * @param departmentId 部门筛选
     * @param roleId 角色筛选
     * @param status 状态筛选
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数
     * @return 员工分页结果
     */
    public PageResult<EnterpriseEmployeeResponse> page(
            Long tenantId,
            String keyword,
            Long departmentId,
            Long roleId,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseEmployeeEntity> wrapper = baseQuery(tenantId)
                .eq(departmentId != null, "department_id", departmentId)
                .eq(roleId != null, "role_id", roleId)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("employee_code", keyword)
                        .or()
                        .like("employee_name", keyword)
                        .or()
                        .like("username", keyword)
                        .or()
                        .like("telephone", keyword)
                        .or()
                        .like("mobile_phone", keyword))
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询员工下拉列表。
     *
     * <p>用于客户单位默认操作计调、业务员工等选择场景。默认只返回启用员工；
     * includeDisabled=true 时包含停用员工，便于编辑历史资料时回显旧值。</p>
     */
    public List<EnterpriseEmployeeResponse> listAll(Long tenantId, boolean includeDisabled) {
        QueryWrapper<EnterpriseEmployeeEntity> wrapper = baseQuery(tenantId)
                .eq(!includeDisabled, "status", EnterpriseEmployeeStatus.ACTIVE.getValue())
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return employeeMapper.selectList(wrapper).stream()
                .map(entity -> toResponse(entity))
                .toList();
    }

    /**
     * 新增员工并创建登录账号。
     *
     * <p>创建顺序先写 system_users，再写 enterprise_employees，最后回填账号的 employee_id。
     * 方法加事务，确保员工资料和登录账号不会只成功一边。</p>
     */
    @Transactional
    public EnterpriseEmployeeResponse create(EnterpriseEmployeeSaveRequest request, Long tenantId, String operator) {
        assertEmployeeUnique(request, tenantId, null);
        assertUsernameAvailable(tenantId, cleanRequired(request.username()), null);
        EnterpriseDepartmentEntity department = assertDepartmentActive(request.departmentId(), tenantId);
        EnterpriseRoleEntity role = assertRoleActive(request.roleId(), tenantId);

        SystemUserEntity user = buildSystemUser(request, tenantId, role, operator);
        systemUserMapper.insert(user);

        EnterpriseEmployeeEntity employee = new EnterpriseEmployeeEntity();
        employee.setTenantId(tenantId);
        employee.setSystemUserId(user.getId());
        applyFields(employee, request, role);
        employee.setCreatedBy(operator);
        employee.setIsDeleted(false);
        employeeMapper.insert(employee);

        SystemUserEntity linked = new SystemUserEntity();
        linked.setEmployeeId(employee.getId());
        systemUserMapper.update(linked, systemUserUpdate(tenantId, user.getId()));
        return toResponse(employee, department, role);
    }

    /**
     * 修改员工资料并同步登录账号。
     *
     * <p>账号名允许修改，但必须保证同租户下员工表和登录账号表都不重复。</p>
     */
    @Transactional
    public EnterpriseEmployeeResponse update(Long id, EnterpriseEmployeeSaveRequest request, Long tenantId) {
        EnterpriseEmployeeEntity current = assertEmployeeExists(id, tenantId);
        assertEmployeeUnique(request, tenantId, id);
        assertUsernameAvailable(tenantId, cleanRequired(request.username()), current.getSystemUserId());
        EnterpriseDepartmentEntity department = assertDepartmentActive(request.departmentId(), tenantId);
        EnterpriseRoleEntity role = assertRoleActive(request.roleId(), tenantId);

        EnterpriseEmployeeStatus status = EnterpriseEmployeeStatus.fromValueOrDefault(request.status());
        int updated = employeeMapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("employee_code", clean(request.employeeCode()))
                .set("employee_name", cleanRequired(request.employeeName()))
                .set("username", cleanRequired(request.username()))
                .set("department_id", request.departmentId())
                .set("role_id", request.roleId())
                .set("gender", EnterpriseEmployeeGender.fromValueOrDefault(request.gender()).getValue())
                .set("telephone", clean(request.telephone()))
                .set("mobile_phone", clean(request.mobilePhone()))
                .set("email", clean(request.email()))
                .set("info_scope", EnterpriseEmployeeScope.fromValueOrDefault(request.infoScope()).getValue())
                .set("profit_scope", EnterpriseEmployeeScope.fromValueOrDefault(request.profitScope()).getValue())
                .set("reception_scope", EnterpriseEmployeeScope.fromValueOrDefault(request.receptionScope()).getValue())
                .set("customer_scope", EnterpriseEmployeeScope.fromValueOrDefault(request.customerScope()).getValue())
                .set("sort_order", number(request.sortOrder()))
                .set("status", status.getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        syncSystemUser(current.getSystemUserId(), tenantId, request, role, status);
        EnterpriseEmployeeEntity saved = employeeMapper.selectOne(baseQuery(tenantId).eq("id", id));
        return toResponse(saved, department, role);
    }

    /**
     * 停用员工并停用关联登录账号。
     *
     * <p>停用不是删除，历史单据仍引用该员工，但账号不能继续登录。</p>
     */
    @Transactional
    public void disable(Long id, Long tenantId) {
        EnterpriseEmployeeEntity current = assertEmployeeExists(id, tenantId);
        EnterpriseEmployeeEntity employee = new EnterpriseEmployeeEntity();
        employee.setStatus(EnterpriseEmployeeStatus.DISABLED.getValue());
        int updated = employeeMapper.update(employee, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        SystemUserEntity user = new SystemUserEntity();
        user.setStatus("disabled");
        systemUserMapper.update(user, systemUserUpdate(tenantId, current.getSystemUserId()));
    }

    /**
     * 重置员工登录密码。
     *
     * <p>重置只更新密码哈希，不改变员工状态、角色和账号其它资料。</p>
     */
    public void resetPassword(Long id, Long tenantId) {
        EnterpriseEmployeeEntity current = assertEmployeeExists(id, tenantId);
        SystemUserEntity user = new SystemUserEntity();
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        int updated = systemUserMapper.update(user, systemUserUpdate(tenantId, current.getSystemUserId()));
        if (updated == 0) {
            throw new BizException("登录账号不存在或已删除");
        }
    }

    /**
     * 软删除员工并同步软删除登录账号。
     *
     * <p>删除员工不会物理移除记录，便于历史业务继续保留人员归属。关联账号同步软删除后不可登录。</p>
     */
    @Override
    @Transactional
    public void delete(Long id, Long tenantId, String operator) {
        EnterpriseEmployeeEntity current = assertEmployeeExists(id, tenantId);
        EnterpriseEmployeeEntity employee = new EnterpriseEmployeeEntity();
        employee.setIsDeleted(true);
        employee.setDeletedAt(OffsetDateTime.now());
        employee.setDeletedBy(operator);
        int updated = employeeMapper.update(employee, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        SystemUserEntity user = new SystemUserEntity();
        user.setIsDeleted(true);
        user.setDeletedAt(OffsetDateTime.now());
        user.setDeletedBy(operator);
        user.setStatus("disabled");
        systemUserMapper.update(user, systemUserUpdate(tenantId, current.getSystemUserId()));
    }

    private void assertEmployeeUnique(EnterpriseEmployeeSaveRequest request, Long tenantId, Long excludeId) {
        assertValueNotExists(tenantId, "employee_code", clean(request.employeeCode()), excludeId, "员工编码已存在");
        assertValueNotExists(tenantId, "username", cleanRequired(request.username()), excludeId, "员工登录账号已存在");
    }

    private void assertUsernameAvailable(Long tenantId, String username, Long currentSystemUserId) {
        Long count = systemUserMapper.selectCount(new QueryWrapper<SystemUserEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("username", username)
                .ne(currentSystemUserId != null, "id", currentSystemUserId));
        if (count != null && count > 0) {
            throw new BizException("登录账号已存在");
        }
    }

    private EnterpriseDepartmentEntity assertDepartmentActive(Long departmentId, Long tenantId) {
        EnterpriseDepartmentEntity department = departmentMapper.selectOne(new QueryWrapper<EnterpriseDepartmentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", departmentId));
        if (department == null) {
            throw new BizException("所属部门不存在或已删除");
        }
        if (!"active".equals(department.getStatus())) {
            throw new BizException("所属部门已停用");
        }
        return department;
    }

    private EnterpriseRoleEntity assertRoleActive(Long roleId, Long tenantId) {
        EnterpriseRoleEntity role = roleMapper.selectOne(new QueryWrapper<EnterpriseRoleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", roleId));
        if (role == null) {
            throw new BizException("角色不存在或已删除");
        }
        if (!"active".equals(role.getStatus())) {
            throw new BizException("角色已停用");
        }
        return role;
    }

    private EnterpriseEmployeeEntity assertEmployeeExists(Long id, Long tenantId) {
        EnterpriseEmployeeEntity employee = employeeMapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (employee == null) {
            throw new BizException(notFoundMessage());
        }
        return employee;
    }

    private SystemUserEntity buildSystemUser(
            EnterpriseEmployeeSaveRequest request,
            Long tenantId,
            EnterpriseRoleEntity role,
            String operator
    ) {
        EnterpriseEmployeeStatus status = EnterpriseEmployeeStatus.fromValueOrDefault(request.status());
        SystemUserEntity user = new SystemUserEntity();
        user.setTenantId(tenantId);
        user.setUsername(cleanRequired(request.username()));
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRealName(cleanRequired(request.employeeName()));
        user.setMobilePhone(clean(request.mobilePhone()));
        user.setEmail(clean(request.email()));
        user.setRoleId(role.getId());
        user.setRoleCode(role.getRoleCode());
        user.setIsTenantAdmin(false);
        user.setStatus(status.getValue());
        user.setCreatedBy(operator);
        user.setRemark(clean(request.remark()));
        user.setIsDeleted(false);
        return user;
    }

    private void syncSystemUser(
            Long systemUserId,
            Long tenantId,
            EnterpriseEmployeeSaveRequest request,
            EnterpriseRoleEntity role,
            EnterpriseEmployeeStatus employeeStatus
    ) {
        SystemUserEntity user = new SystemUserEntity();
        user.setUsername(cleanRequired(request.username()));
        user.setRealName(cleanRequired(request.employeeName()));
        user.setMobilePhone(clean(request.mobilePhone()));
        user.setEmail(clean(request.email()));
        user.setRoleId(role.getId());
        user.setRoleCode(role.getRoleCode());
        user.setStatus(employeeStatus.getValue());
        user.setRemark(clean(request.remark()));
        int updated = systemUserMapper.update(user, systemUserUpdate(tenantId, systemUserId));
        if (updated == 0) {
            throw new BizException("登录账号不存在或已删除");
        }
    }

    private void applyFields(EnterpriseEmployeeEntity entity, EnterpriseEmployeeSaveRequest request, EnterpriseRoleEntity role) {
        EnterpriseEmployeeStatus status = EnterpriseEmployeeStatus.fromValueOrDefault(request.status());
        entity.setEmployeeCode(clean(request.employeeCode()));
        entity.setEmployeeName(cleanRequired(request.employeeName()));
        entity.setUsername(cleanRequired(request.username()));
        entity.setDepartmentId(request.departmentId());
        entity.setRoleId(role.getId());
        entity.setGender(EnterpriseEmployeeGender.fromValueOrDefault(request.gender()).getValue());
        entity.setTelephone(clean(request.telephone()));
        entity.setMobilePhone(clean(request.mobilePhone()));
        entity.setEmail(clean(request.email()));
        entity.setInfoScope(EnterpriseEmployeeScope.fromValueOrDefault(request.infoScope()).getValue());
        entity.setProfitScope(EnterpriseEmployeeScope.fromValueOrDefault(request.profitScope()).getValue());
        entity.setReceptionScope(EnterpriseEmployeeScope.fromValueOrDefault(request.receptionScope()).getValue());
        entity.setCustomerScope(EnterpriseEmployeeScope.fromValueOrDefault(request.customerScope()).getValue());
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setRemark(clean(request.remark()));
    }

    private UpdateWrapper<SystemUserEntity> systemUserUpdate(Long tenantId, Long systemUserId) {
        return new UpdateWrapper<SystemUserEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", systemUserId);
    }

    @Override
    protected EnterpriseEmployeeEntity newEntity() {
        return new EnterpriseEmployeeEntity();
    }

    @Override
    protected EnterpriseEmployeeResponse toResponse(EnterpriseEmployeeEntity entity) {
        EnterpriseDepartmentEntity department = entity.getDepartmentId() == null
                ? null
                : departmentMapper.selectOne(new QueryWrapper<EnterpriseDepartmentEntity>()
                        .eq("tenant_id", entity.getTenantId())
                        .eq("is_deleted", false)
                        .eq("id", entity.getDepartmentId()));
        EnterpriseRoleEntity role = entity.getRoleId() == null
                ? null
                : roleMapper.selectOne(new QueryWrapper<EnterpriseRoleEntity>()
                        .eq("tenant_id", entity.getTenantId())
                        .eq("is_deleted", false)
                        .eq("id", entity.getRoleId()));
        return toResponse(entity, department, role);
    }

    private EnterpriseEmployeeResponse toResponse(
            EnterpriseEmployeeEntity employee,
            EnterpriseDepartmentEntity department,
            EnterpriseRoleEntity role
    ) {
        return EnterpriseEmployeeResponse.fromEntity(
                employee,
                department == null ? null : department.getDepartmentName(),
                role == null ? null : role.getRoleCode(),
                role == null ? null : role.getRoleName()
        );
    }

    @Override
    protected String notFoundMessage() {
        return "员工不存在或已删除";
    }
}
