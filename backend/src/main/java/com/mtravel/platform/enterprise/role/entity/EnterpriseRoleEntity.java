package com.mtravel.platform.enterprise.role.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业角色实体。
 *
 * <p>角色用于承接员工账号的角色归属，并作为菜单权限、按钮权限和数据权限的配置入口。
 * 角色删除采用软删除，避免历史员工和操作日志失去角色含义。</p>
 */
@TableName("enterprise_roles")
public class EnterpriseRoleEntity extends TenantSoftDeleteEntity {

    /** 角色编码，用于登录令牌、权限判断和外部数据导入匹配。 */
    @TableField("role_code")
    private String roleCode;

    /** 角色名称，例如管理员、销售、计调、财务、总经理。 */
    @TableField("role_name")
    private String roleName;

    /** 排序值。数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否系统内置角色，内置角色默认不建议删除。 */
    @TableField("system_builtin")
    private Boolean systemBuiltin;

    /** 角色状态。active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getSystemBuiltin() {
        return systemBuiltin;
    }

    public void setSystemBuiltin(Boolean systemBuiltin) {
        this.systemBuiltin = systemBuiltin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
