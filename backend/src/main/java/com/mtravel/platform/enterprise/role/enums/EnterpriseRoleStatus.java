package com.mtravel.platform.enterprise.role.enums;

/**
 * 企业角色状态枚举。
 *
 * <p>数据库 status 字段使用 varchar 保存，代码层必须通过枚举集中管理允许值，避免 Service 中散落裸字符串。</p>
 */
public enum EnterpriseRoleStatus {

    /** 启用状态，员工可分配该角色，登录账号可使用该角色编码。 */
    ACTIVE("active"),

    /** 停用状态，角色保留历史引用，但不建议继续分配给新员工。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseRoleStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将前端传入的状态值转换为枚举，空值按启用处理。
     *
     * @param value 前端传入的状态值
     * @return 合法角色状态
     */
    public static EnterpriseRoleStatus fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        for (EnterpriseRoleStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("角色状态不合法");
    }
}
