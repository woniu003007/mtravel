package com.mtravel.platform.configuration.quote.enums;

/**
 * 销售报价审批配置人员类型。
 */
public enum QuoteApprovalMemberType {
    /** 审批人，按 step_order 顺序处理。 */
    APPROVER("approver"),
    /** 抄送人，审批通过后可见。 */
    CC("cc");

    private final String value;

    QuoteApprovalMemberType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
