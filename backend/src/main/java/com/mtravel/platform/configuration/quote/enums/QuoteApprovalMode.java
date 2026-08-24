package com.mtravel.platform.configuration.quote.enums;

import com.mtravel.platform.common.BizException;

/**
 * 销售报价低价审批模式。
 */
public enum QuoteApprovalMode {
    /** 按发起审批的当前登录账号所属部门负责人审批。 */
    DEPARTMENT_MANAGER("department_manager"),
    /** 按配置中指定的具体人员顺序审批。 */
    SPECIFIED_PERSON("specified_person");

    private final String value;

    QuoteApprovalMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static QuoteApprovalMode fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return SPECIFIED_PERSON;
        }
        for (QuoteApprovalMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new BizException("报价审批方式不合法");
    }
}
