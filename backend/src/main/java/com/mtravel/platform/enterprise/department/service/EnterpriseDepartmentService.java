package com.mtravel.platform.enterprise.department.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.department.dto.EnterpriseDepartmentResponse;
import com.mtravel.platform.enterprise.department.dto.EnterpriseDepartmentSaveRequest;
import com.mtravel.platform.enterprise.department.entity.EnterpriseDepartmentEntity;
import com.mtravel.platform.enterprise.department.enums.EnterpriseDepartmentStatus;
import com.mtravel.platform.enterprise.department.mapper.EnterpriseDepartmentMapper;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 企业部门业务服务。
 *
 * <p>这里集中处理租户隔离、软删除、部门名称/编码唯一性、上级部门有效性和删除保护。
 * 部门会被员工、权限和统计引用，因此删除只能软删除，并且有下级部门时必须先处理下级部门。</p>
 */
@Service
public class EnterpriseDepartmentService extends BusinessCrudService<EnterpriseDepartmentEntity, EnterpriseDepartmentResponse> {

    private final EnterpriseDepartmentMapper mapper;
    private final EnterpriseEmployeeMapper employeeMapper;

    public EnterpriseDepartmentService(EnterpriseDepartmentMapper mapper) {
        this(mapper, null);
    }

    @Autowired
    public EnterpriseDepartmentService(
            EnterpriseDepartmentMapper mapper,
            EnterpriseEmployeeMapper employeeMapper
    ) {
        super(mapper);
        this.mapper = mapper;
        this.employeeMapper = employeeMapper;
    }

    /**
     * 分页查询部门。
     *
     * <p>keyword 同时匹配部门名称、部门编码、负责人和联系电话。status 为空时查询全部未删除部门。</p>
     */
    public PageResult<EnterpriseDepartmentResponse> page(
            Long tenantId,
            String keyword,
            String status,
            Long parentId,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseDepartmentEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(parentId != null, "parent_id", parentId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("department_name", keyword)
                        .or()
                        .like("department_code", keyword)
                        .or()
                        .like("manager_name", keyword)
                        .or()
                        .like("contact_phone", keyword))
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询全部未删除部门。
     *
     * <p>用于部门下拉、上级部门选择和员工归属选择。includeDisabled=true 时返回停用部门，便于编辑历史数据。</p>
     */
    public List<EnterpriseDepartmentResponse> listAll(Long tenantId, boolean includeDisabled) {
        QueryWrapper<EnterpriseDepartmentEntity> wrapper = baseQuery(tenantId)
                .eq(!includeDisabled, "status", EnterpriseDepartmentStatus.ACTIVE.getValue())
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return toResponses(mapper.selectList(wrapper));
    }

    /**
     * 新增部门。
     *
     * <p>新增时校验同租户下未删除部门名称和编码不重复，并确认上级部门属于当前租户。</p>
     */
    public EnterpriseDepartmentResponse create(
            EnterpriseDepartmentSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertUnique(request, tenantId, null);
        assertParentValid(tenantId, request.parentId(), null);
        if (request.managerEmployeeId() != null) {
            throw new BizException("新增部门后才能选择本部门负责人");
        }

        EnterpriseDepartmentEntity entity = new EnterpriseDepartmentEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改部门。
     *
     * <p>修改上级部门时，不允许把自己或自己的下级部门设为上级，避免组织架构形成循环。
     * 更新使用显式 set，保证前端清空上级部门、负责人、电话、备注时也能写回数据库。</p>
     */
    public EnterpriseDepartmentResponse update(
            Long id,
            EnterpriseDepartmentSaveRequest request,
            Long tenantId
    ) {
        assertUnique(request, tenantId, id);
        assertParentValid(tenantId, request.parentId(), id);
        EnterpriseEmployeeEntity manager = findManagerEmployee(request.managerEmployeeId(), id, tenantId);

        EnterpriseDepartmentStatus status = EnterpriseDepartmentStatus.fromValueOrDefault(request.status());
        int updated = mapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("parent_id", request.parentId())
                .set("department_code", clean(request.departmentCode()))
                .set("department_name", cleanRequired(request.departmentName()))
                .set("manager_employee_id", request.managerEmployeeId())
                .set("manager_name", manager == null ? clean(request.managerName()) : manager.getEmployeeName())
                .set("contact_phone", clean(request.contactPhone()))
                .set("sort_order", number(request.sortOrder()))
                .set("status", status.getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /**
     * 软删除部门。
     *
     * <p>存在未删除下级部门时禁止删除，避免组织树出现孤儿节点。后续员工引用校验接入员工模块后再补。</p>
     */
    @Override
    public void delete(Long id, Long tenantId, String operator) {
        Long childCount = mapper.selectCount(baseQuery(tenantId).eq("parent_id", id));
        if (childCount != null && childCount > 0) {
            throw new BizException("请先处理下级部门后再删除");
        }
        super.delete(id, tenantId, operator);
    }

    private void assertUnique(EnterpriseDepartmentSaveRequest request, Long tenantId, Long excludeId) {
        assertValueNotExists(
                tenantId,
                "department_name",
                cleanRequired(request.departmentName()),
                excludeId,
                "部门名称已存在"
        );
        assertValueNotExists(
                tenantId,
                "department_code",
                clean(request.departmentCode()),
                excludeId,
                "部门编码已存在"
        );
    }

    private void assertParentValid(Long tenantId, Long parentId, Long currentId) {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(parentId, currentId)) {
            throw new BizException("上级部门不能选择自己");
        }
        Long cursor = parentId;
        int guard = 0;
        while (cursor != null) {
            if (guard++ > 1000) {
                throw new BizException("部门层级异常，请检查上级部门设置");
            }
            if (Objects.equals(cursor, currentId)) {
                throw new BizException("上级部门不能选择自己的下级部门");
            }
            EnterpriseDepartmentEntity parent = mapper.selectOne(baseQuery(tenantId).eq("id", cursor));
            if (parent == null) {
                throw new BizException("上级部门不存在或已删除");
            }
            cursor = parent.getParentId();
        }
    }

    private void applyFields(EnterpriseDepartmentEntity entity, EnterpriseDepartmentSaveRequest request) {
        EnterpriseDepartmentStatus status = EnterpriseDepartmentStatus.fromValueOrDefault(request.status());
        entity.setParentId(request.parentId());
        entity.setDepartmentCode(clean(request.departmentCode()));
        entity.setDepartmentName(cleanRequired(request.departmentName()));
        entity.setManagerEmployeeId(null);
        entity.setManagerName(clean(request.managerName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setRemark(clean(request.remark()));
    }

    private EnterpriseEmployeeEntity findManagerEmployee(Long managerEmployeeId, Long departmentId, Long tenantId) {
        if (managerEmployeeId == null) {
            return null;
        }
        if (employeeMapper == null) {
            throw new BizException("部门负责人账号功能未初始化");
        }
        EnterpriseEmployeeEntity manager = employeeMapper.selectOne(new QueryWrapper<EnterpriseEmployeeEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", managerEmployeeId)
                .eq("department_id", departmentId)
                .eq("status", "active")
                .eq("is_deleted", false));
        if (manager == null || manager.getSystemUserId() == null) {
            throw new BizException("部门负责人必须是本部门的启用员工并已绑定登录账号");
        }
        return manager;
    }

    private List<EnterpriseDepartmentResponse> toResponses(List<EnterpriseDepartmentEntity> entities) {
        Map<Long, EnterpriseDepartmentEntity> entityById = entities.stream()
                .collect(Collectors.toMap(EnterpriseDepartmentEntity::getId, Function.identity(), (left, right) -> left));
        return entities.stream()
                .map(entity -> EnterpriseDepartmentResponse.fromEntity(
                        entity,
                        entity.getParentId() == null || !entityById.containsKey(entity.getParentId())
                                ? null
                                : entityById.get(entity.getParentId()).getDepartmentName()
                ))
                .toList();
    }

    @Override
    protected EnterpriseDepartmentEntity newEntity() {
        return new EnterpriseDepartmentEntity();
    }

    @Override
    protected EnterpriseDepartmentResponse toResponse(EnterpriseDepartmentEntity entity) {
        String parentName = null;
        if (entity.getParentId() != null) {
            EnterpriseDepartmentEntity parent = mapper.selectOne(baseQuery(entity.getTenantId()).eq("id", entity.getParentId()));
            parentName = parent == null ? null : parent.getDepartmentName();
        }
        return EnterpriseDepartmentResponse.fromEntity(entity, parentName);
    }

    @Override
    protected String notFoundMessage() {
        return "部门不存在或已删除";
    }
}
