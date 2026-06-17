package com.mtravel.platform.enterprise.employee.enums;

/**
 * 企业员工性别枚举。
 *
 * <p>性别不是核心权限字段，但落库仍需约束取值，便于员工名录筛选和后续导入校验。</p>
 */
public enum EnterpriseEmployeeGender {

    /** 男。 */
    MALE("male"),

    /** 女。 */
    FEMALE("female"),

    /** 未填写或未知。 */
    UNKNOWN("unknown");

    private final String value;

    EnterpriseEmployeeGender(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将前端传入性别转换为枚举，空值按未知处理。
     *
     * @param value 前端传入性别
     * @return 合法性别
     */
    public static EnterpriseEmployeeGender fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (EnterpriseEmployeeGender gender : values()) {
            if (gender.value.equals(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("员工性别不合法");
    }
}
