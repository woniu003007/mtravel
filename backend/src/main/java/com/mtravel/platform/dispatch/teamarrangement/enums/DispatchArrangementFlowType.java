package com.mtravel.platform.dispatch.teamarrangement.enums;

/**
 * 团队安排下游流程类型。
 *
 * <p>当前先记录导游报账同步和计调审核同步，后续可扩展财务审核流水。</p>
 */
public enum DispatchArrangementFlowType {
    /** 导游报账阶段记录。 */
    GUIDE_REPORT("guide_report"),
    /** 计调审核阶段记录。 */
    OPERATOR_AUDIT("operator_audit");

    private final String value;

    DispatchArrangementFlowType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
