package com.mtravel.platform.enterprise.role.dto;

import com.mtravel.platform.enterprise.role.entity.EnterpriseRoleEntity;
import java.time.OffsetDateTime;

/**
 * 企业角色返回对象。
 *
 * <p>角色列表、详情和员工角色下拉共用该对象。employeeCount 用于前端判断角色是否可删除。</p>
 */
public record EnterpriseRoleResponse(
        Long id,
        String roleCode,
        String roleName,
        Integer sortOrder,
        Boolean systemBuiltin,
        String status,
        Long employeeCount,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将角色实体转换为接口返回对象。 */
    public static EnterpriseRoleResponse fromEntity(EnterpriseRoleEntity entity, Long employeeCount) {
        return new EnterpriseRoleResponse(
                entity.getId(),
                entity.getRoleCode(),
                entity.getRoleName(),
                entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getSystemBuiltin()),
                entity.getStatus(),
                employeeCount == null ? 0L : employeeCount,
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
