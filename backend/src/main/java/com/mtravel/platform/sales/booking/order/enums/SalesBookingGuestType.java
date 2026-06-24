package com.mtravel.platform.sales.booking.order.enums;

import java.util.Arrays;

/**
 * 收客订单游客类型枚举。
 *
 * <p>游客类型同时用于订单人数统计和价格明细匹配，必须集中管理，避免成人、儿童不占床等字段
 * 在 Service 中散落成字符串。</p>
 */
public enum SalesBookingGuestType {

    /** 成人游客。 */
    ADULT("adult", "成人"),

    /** 儿童占床。 */
    CHILD("child", "儿童"),

    /** 儿童不占床。 */
    CHILD_NO_BED("child_no_bed", "儿童不占床"),

    /** 老人游客。 */
    SENIOR("senior", "老人"),

    /** 全陪人员。 */
    ESCORT("escort", "全陪");

    private final String value;
    private final String label;

    SalesBookingGuestType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** 校验游客类型是否合法。 */
    public static boolean valid(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}
