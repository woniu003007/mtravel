package com.mtravel.platform.sales.booking.order.enums;

import java.util.Arrays;

/**
 * 销售收客订单业务角色枚举。
 *
 * <p>订单角色用于区分普通订单、历史拼团来源留痕订单和拼团目标子订单。
 * 新拼团不再写入 merge_source；历史 merge_source 按老系统复测口径继续参与来源团人数、收入和毛利统计。</p>
 */
public enum SalesBookingOrderRole {

    /** 普通订单，正常参与团队人数、收入和游客名单统计。 */
    NORMAL("normal", "普通订单"),

    /** 历史已拼出来源订单，保留追溯关系并继续参与来源团队统计。 */
    MERGE_SOURCE("merge_source", "已拼出"),

    /** 拼团目标子订单，参与目标团队统计和执行。 */
    MERGE_CHILD("merge_child", "拼入订单");

    private final String value;
    private final String label;

    SalesBookingOrderRole(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** 校验订单角色是否合法。 */
    public static boolean valid(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}
