package com.mtravel.platform.dispatch.guide.enums;

/**
 * 导游请假审批状态。
 */
public enum GuideLeaveStatus {
    /** 待审批，仅用于提醒，不阻断排团。 */
    PENDING("pending"),
    /** 已通过，会进入排班和冲突判断。 */
    APPROVED("approved"),
    /** 已驳回，不进入排班和冲突判断。 */
    REJECTED("rejected"),
    /** 已撤回，不进入排班和冲突判断。 */
    WITHDRAWN("withdrawn"),
    /** 已取消，不进入排班和冲突判断。 */
    CANCELLED("cancelled");

    private final String value;

    GuideLeaveStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
