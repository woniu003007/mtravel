package com.mtravel.platform.dispatch.guide.enums;

/**
 * 导游请假来源类型。
 */
public enum GuideLeaveSourceType {
    /** 导游自己提交请假申请，需要后台审批后生效。 */
    GUIDE_APPLY("guide_apply"),
    /** 计调直接设置导游不可上团，保存后直接生效。 */
    DISPATCHER_DIRECT("dispatcher_direct");

    private final String value;

    GuideLeaveSourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
