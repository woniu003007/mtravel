package com.mtravel.platform.dispatch.teamarrangement.enums;

import com.mtravel.platform.common.BizException;

/**
 * 团队安排成本归属模式。
 *
 * <p>该枚举承接旧系统“全团/订单均摊”和“多订单均摊成本”两个页签。</p>
 */
public enum DispatchArrangementAllocationMode {
    /** 全团公共成本或单订单归属。 */
    GROUP_ORDER_AVERAGE("group_order_average"),
    /** 一次录入后拆分到多个订单。 */
    MULTI_ORDER_AVERAGE("multi_order_average");

    private final String value;

    DispatchArrangementAllocationMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 解析成本归属模式，默认使用全团/订单均摊。 */
    public static DispatchArrangementAllocationMode fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return GROUP_ORDER_AVERAGE;
        }
        for (DispatchArrangementAllocationMode item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("费用归属模式不合法");
    }
}
