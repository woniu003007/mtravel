package com.mtravel.platform.dispatch.guide.enums;

/**
 * 团队导游安排状态。
 */
public enum DispatchTeamGuideStatus {
    /** 生效中的导游安排，会进入排班和冲突判断。 */
    ACTIVE("active"),
    /** 已取消的导游安排，不再进入排班和冲突判断。 */
    CANCELLED("cancelled");

    private final String value;

    DispatchTeamGuideStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
