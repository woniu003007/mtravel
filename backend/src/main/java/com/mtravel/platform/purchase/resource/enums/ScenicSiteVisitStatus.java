package com.mtravel.platform.purchase.resource.enums;

import java.util.Set;

/** 景区踩点维护状态，用于区分历史未整理、明确未踩点和已经踩点。 */
public enum ScenicSiteVisitStatus {
    /** 历史数据或尚未整理。 */
    UNMAINTAINED("unmaintained"),
    /** 已明确确认尚未踩点。 */
    NOT_VISITED("not_visited"),
    /** 已完成踩点。 */
    VISITED("visited");

    private static final Set<String> VALUES = Set.of("unmaintained", "not_visited", "visited");

    private final String value;

    ScenicSiteVisitStatus(String value) {
        this.value = value;
    }

    /** 判断接口入参是否属于允许的踩点状态。 */
    public static boolean contains(String value) {
        return VALUES.contains(value);
    }

    /** 返回接口和数据库使用的稳定值。 */
    public String value() {
        return value;
    }
}
