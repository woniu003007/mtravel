package com.mtravel.platform.purchase.resource.enums;

import java.util.Set;

/** 酒店和餐厅资源的星级/档次筛选值。 */
public enum ResourceStarLevel {
    /** 未评级或尚未维护。 */
    UNRATED("unrated"),
    /** 一星。 */
    ONE_STAR("1star"),
    /** 二星。 */
    TWO_STAR("2star"),
    /** 三星。 */
    THREE_STAR("3star"),
    /** 四星。 */
    FOUR_STAR("4star"),
    /** 五星。 */
    FIVE_STAR("5star");

    private static final Set<String> VALUES = Set.of("unrated", "1star", "2star", "3star", "4star", "5star");

    private final String value;

    ResourceStarLevel(String value) {
        this.value = value;
    }

    /** 判断接口入参是否属于允许的星级筛选值。 */
    public static boolean contains(String value) {
        return VALUES.contains(value);
    }

    /** 返回数据库和接口使用的稳定值。 */
    public String value() {
        return value;
    }
}
