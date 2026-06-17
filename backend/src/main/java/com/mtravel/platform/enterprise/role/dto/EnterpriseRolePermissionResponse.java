package com.mtravel.platform.enterprise.role.dto;

import com.mtravel.platform.enterprise.role.entity.EnterpriseRolePermissionEntity;

/**
 * 企业角色权限返回对象。
 *
 * <p>权限管理抽屉读取该对象后，按模块编码分组展示已勾选权限。</p>
 */
public record EnterpriseRolePermissionResponse(
        Long id,
        Long roleId,
        String moduleCode,
        String moduleName,
        String permissionCode,
        String permissionName,
        String permissionType,
        Integer sortOrder
) {

    /** 将权限实体转换为接口返回对象。 */
    public static EnterpriseRolePermissionResponse fromEntity(EnterpriseRolePermissionEntity entity) {
        return new EnterpriseRolePermissionResponse(
                entity.getId(),
                entity.getRoleId(),
                entity.getModuleCode(),
                entity.getModuleName(),
                entity.getPermissionCode(),
                entity.getPermissionName(),
                entity.getPermissionType(),
                entity.getSortOrder()
        );
    }
}
