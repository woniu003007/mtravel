package com.mtravel.platform.enterprise.role.enums;

/**
 * 企业角色权限类型枚举。
 *
 * <p>首版权限管理先保存权限入口配置，后续菜单、按钮和数据范围拦截会复用这些类型。</p>
 */
public enum EnterprisePermissionType {

    /** 菜单权限，用于控制菜单是否展示或可访问。 */
    MENU("menu"),

    /** 按钮权限，用于控制新增、修改、删除、审核等操作入口。 */
    BUTTON("button"),

    /** 数据权限，用于控制全公司、部门或个人等数据范围。 */
    DATA("data");

    private final String value;

    EnterprisePermissionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将前端传入的权限类型转换为枚举，空值按菜单权限处理。
     *
     * @param value 前端传入的权限类型
     * @return 合法权限类型
     */
    public static EnterprisePermissionType fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return MENU;
        }
        for (EnterprisePermissionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("权限类型不合法");
    }
}
