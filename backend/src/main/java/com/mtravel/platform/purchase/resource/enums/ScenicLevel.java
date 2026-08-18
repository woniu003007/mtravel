package com.mtravel.platform.purchase.resource.enums;

import java.util.Set;

/**
 * 景区国家 A 级。
 *
 * <p>未取得或尚未维护国家 A 级时使用 {@code unrated}，避免把历史景区误标为已评级。</p>
 */
public enum ScenicLevel {
    /** 未评级或尚未维护。 */
    UNRATED("unrated"),
    /** 国家 1A 级景区。 */
    LEVEL_1A("1a"),
    /** 国家 2A 级景区。 */
    LEVEL_2A("2a"),
    /** 国家 3A 级景区。 */
    LEVEL_3A("3a"),
    /** 国家 4A 级景区。 */
    LEVEL_4A("4a"),
    /** 国家 5A 级景区。 */
    LEVEL_5A("5a");

    private static final Set<String> VALUES = Set.of("unrated", "1a", "2a", "3a", "4a", "5a");

    private final String value;

    ScenicLevel(String value) {
        this.value = value;
    }

    /** 判断接口入参是否属于允许的景区等级。 */
    public static boolean contains(String value) {
        return VALUES.contains(value);
    }

    /** 返回接口和数据库使用的稳定值。 */
    public String value() {
        return value;
    }
}
