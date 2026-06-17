package com.mtravel.platform.enterprise.employee.enums;

/**
 * 企业员工数据查看范围枚举。
 *
 * <p>员工管理保留信息、利润、收客和客户四类查看范围。当前先保存配置，后续权限拦截再按这些字段控制数据边界。</p>
 */
public enum EnterpriseEmployeeScope {

    /** 全公司范围。 */
    COMPANY("company"),

    /** 所属部门范围。 */
    DEPARTMENT("department"),

    /** 个人范围。 */
    PERSONAL("personal");

    private final String value;

    EnterpriseEmployeeScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将前端传入的数据范围转换为枚举，空值按个人范围处理。
     *
     * @param value 前端传入的数据范围
     * @return 合法数据范围
     */
    public static EnterpriseEmployeeScope fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return PERSONAL;
        }
        for (EnterpriseEmployeeScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("员工查看范围不合法");
    }
}
