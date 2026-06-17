package com.mtravel.platform.enterprise.role.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRolePermissionResponse;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRolePermissionSaveRequest;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRoleResponse;
import com.mtravel.platform.enterprise.role.dto.EnterpriseRoleSaveRequest;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRoleEntity;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRolePermissionEntity;
import com.mtravel.platform.enterprise.role.enums.EnterprisePermissionType;
import com.mtravel.platform.enterprise.role.enums.EnterpriseRoleStatus;
import com.mtravel.platform.enterprise.role.mapper.EnterpriseRoleMapper;
import com.mtravel.platform.enterprise.role.mapper.EnterpriseRolePermissionMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 企业角色业务服务。
 *
 * <p>角色管理集中处理租户隔离、软删除、角色编码/名称唯一性、员工引用保护和权限配置替换。
 * Controller 不直接操作 Mapper，避免权限规则散落到接口层。</p>
 */
@Service
public class EnterpriseRoleService extends BusinessCrudService<EnterpriseRoleEntity, EnterpriseRoleResponse> {

    private final EnterpriseRoleMapper roleMapper;
    private final EnterpriseRolePermissionMapper permissionMapper;
    private final EnterpriseEmployeeMapper employeeMapper;

    public EnterpriseRoleService(
            EnterpriseRoleMapper roleMapper,
            EnterpriseRolePermissionMapper permissionMapper,
            EnterpriseEmployeeMapper employeeMapper
    ) {
        super(roleMapper);
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.employeeMapper = employeeMapper;
    }

    /**
     * 分页查询角色。
     *
     * @param tenantId 当前租户 ID
     * @param keyword 角色编码或角色名称关键字
     * @param status 状态筛选，可为空
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数
     * @return 分页角色列表
     */
    public PageResult<EnterpriseRoleResponse> page(
            Long tenantId,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseRoleEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("role_code", keyword)
                        .or()
                        .like("role_name", keyword))
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询全部角色。
     *
     * @param tenantId 当前租户 ID
     * @param includeDisabled 是否包含停用角色
     * @return 角色下拉列表
     */
    public List<EnterpriseRoleResponse> listAll(Long tenantId, boolean includeDisabled) {
        QueryWrapper<EnterpriseRoleEntity> wrapper = baseQuery(tenantId)
                .eq(!includeDisabled, "status", EnterpriseRoleStatus.ACTIVE.getValue())
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return roleMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    /**
     * 新增角色。
     *
     * <p>新增时校验角色编码和名称在同租户未删除数据中唯一。</p>
     */
    public EnterpriseRoleResponse create(EnterpriseRoleSaveRequest request, Long tenantId, String operator) {
        assertUnique(request, tenantId, null);
        EnterpriseRoleEntity entity = new EnterpriseRoleEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setSystemBuiltin(false);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        roleMapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改角色。
     *
     * <p>角色编码会影响后续权限判断和登录令牌。修改时仍允许调整，但必须保持同租户唯一。</p>
     */
    public EnterpriseRoleResponse update(Long id, EnterpriseRoleSaveRequest request, Long tenantId) {
        assertUnique(request, tenantId, id);
        EnterpriseRoleStatus status = EnterpriseRoleStatus.fromValueOrDefault(request.status());
        int updated = roleMapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("role_code", cleanRequired(request.roleCode()))
                .set("role_name", cleanRequired(request.roleName()))
                .set("sort_order", number(request.sortOrder()))
                .set("status", status.getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /**
     * 软删除角色。
     *
     * <p>已被员工引用的角色不能删除，否则员工列表、登录账号和历史操作日志会失去角色含义。</p>
     */
    @Override
    public void delete(Long id, Long tenantId, String operator) {
        Long usedCount = employeeMapper.selectCount(new QueryWrapper<EnterpriseEmployeeEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("role_id", id));
        if (usedCount != null && usedCount > 0) {
            throw new BizException("角色已被员工使用，不能删除");
        }
        super.delete(id, tenantId, operator);
    }

    /**
     * 查询角色已分配权限。
     *
     * @param roleId 角色 ID
     * @param tenantId 当前租户 ID
     * @return 权限列表
     */
    public List<EnterpriseRolePermissionResponse> listPermissions(Long roleId, Long tenantId) {
        assertRoleExists(roleId, tenantId);
        return permissionMapper.selectList(new QueryWrapper<EnterpriseRolePermissionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("role_id", roleId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(EnterpriseRolePermissionResponse::fromEntity)
                .toList();
    }

    /**
     * 保存角色权限。
     *
     * <p>权限界面按角色整体提交。为了避免前端增删差异计算出错，后端先软删除旧权限，再插入当前提交权限。</p>
     */
    @Transactional
    public void savePermissions(
            Long roleId,
            EnterpriseRolePermissionSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertRoleExists(roleId, tenantId);
        EnterpriseRolePermissionEntity deleted = new EnterpriseRolePermissionEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(OffsetDateTime.now());
        deleted.setDeletedBy(operator);
        permissionMapper.update(deleted, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<EnterpriseRolePermissionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("role_id", roleId));
        for (EnterpriseRolePermissionSaveRequest.PermissionItem item : request.permissions()) {
            EnterpriseRolePermissionEntity entity = new EnterpriseRolePermissionEntity();
            entity.setTenantId(tenantId);
            entity.setRoleId(roleId);
            entity.setModuleCode(cleanRequired(item.moduleCode()));
            entity.setModuleName(cleanRequired(item.moduleName()));
            entity.setPermissionCode(cleanRequired(item.permissionCode()));
            entity.setPermissionName(cleanRequired(item.permissionName()));
            entity.setPermissionType(EnterprisePermissionType.fromValueOrDefault(item.permissionType()).getValue());
            entity.setSortOrder(number(item.sortOrder()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            permissionMapper.insert(entity);
        }
    }

    private void assertUnique(EnterpriseRoleSaveRequest request, Long tenantId, Long excludeId) {
        assertValueNotExists(tenantId, "role_code", cleanRequired(request.roleCode()), excludeId, "角色编码已存在");
        assertValueNotExists(tenantId, "role_name", cleanRequired(request.roleName()), excludeId, "角色名称已存在");
    }

    private void assertRoleExists(Long roleId, Long tenantId) {
        EnterpriseRoleEntity role = roleMapper.selectOne(baseQuery(tenantId).eq("id", roleId));
        if (role == null) {
            throw new BizException(notFoundMessage());
        }
    }

    private void applyFields(EnterpriseRoleEntity entity, EnterpriseRoleSaveRequest request) {
        EnterpriseRoleStatus status = EnterpriseRoleStatus.fromValueOrDefault(request.status());
        entity.setRoleCode(cleanRequired(request.roleCode()));
        entity.setRoleName(cleanRequired(request.roleName()));
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setRemark(clean(request.remark()));
    }

    @Override
    protected EnterpriseRoleEntity newEntity() {
        return new EnterpriseRoleEntity();
    }

    @Override
    protected EnterpriseRoleResponse toResponse(EnterpriseRoleEntity entity) {
        Long employeeCount = employeeMapper.selectCount(new QueryWrapper<EnterpriseEmployeeEntity>()
                .eq("tenant_id", entity.getTenantId())
                .eq("is_deleted", false)
                .eq("role_id", entity.getId()));
        return EnterpriseRoleResponse.fromEntity(entity, employeeCount);
    }

    @Override
    protected String notFoundMessage() {
        return "角色不存在或已删除";
    }
}
