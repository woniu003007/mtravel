package com.mtravel.platform.enterprise.employee.enums;

/**
 * 企业员工状态枚举。
 *
 * <p>员工状态会同步影响登录账号状态，因此统一用枚举表达，避免出现员工启用但账号停用等不可控状态。</p>
 */
public enum EnterpriseEmployeeStatus {

    /** 启用状态，员工可参与业务分配，关联登录账号可登录。 */
    ACTIVE("active"),

    /** 停用状态，员工保留历史引用，关联登录账号不可登录。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseEmployeeStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将前端传入状态转换为枚举，空值按启用处理。
     *
     * @param value 前端传入状态
     * @return 合法员工状态
     */
    public static EnterpriseEmployeeStatus fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        for (EnterpriseEmployeeStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("员工状态不合法");
    }
}
