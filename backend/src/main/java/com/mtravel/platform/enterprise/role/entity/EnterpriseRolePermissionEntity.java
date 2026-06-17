package com.mtravel.platform.enterprise.role.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业角色权限实体。
 *
 * <p>首版用于保存角色权限管理界面的勾选结果。权限编码后续可被菜单过滤、按钮控制和数据范围控制复用。</p>
 */
@TableName("enterprise_role_permissions")
public class EnterpriseRolePermissionEntity extends TenantSoftDeleteEntity {

    /** 角色 ID，关联 enterprise_roles。 */
    @TableField("role_id")
    private Long roleId;

    /** 模块编码，用于按业务模块分组展示权限。 */
    @TableField("module_code")
    private String moduleCode;

    /** 模块名称，例如客户管理、销售管理、计调操作、财务管理。 */
    @TableField("module_name")
    private String moduleName;

    /** 权限编码，用于菜单、按钮或数据范围判断。 */
    @TableField("permission_code")
    private String permissionCode;

    /** 权限名称，用于权限管理界面展示。 */
    @TableField("permission_name")
    private String permissionName;

    /** 权限类型。menu 表示菜单，button 表示按钮，data 表示数据范围。 */
    @TableField("permission_type")
    private String permissionType;

    /** 排序值。数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
