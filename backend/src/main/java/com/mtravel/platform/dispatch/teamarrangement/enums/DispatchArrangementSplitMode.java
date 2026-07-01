package com.mtravel.platform.dispatch.teamarrangement.enums;

import com.mtravel.platform.common.BizException;

/**
 * 多订单均摊拆分方式。
 *
 * <p>多订单均摊保存时按该枚举把一次录入的总成本拆成多条单订单成本。</p>
 */
public enum DispatchArrangementSplitMode {
    /** 按订单数量平均拆分。 */
    BY_ORDER("by_order"),
    /** 按各订单游客人数比例拆分。 */
    BY_PEOPLE("by_people");

    private final String value;

    DispatchArrangementSplitMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 解析多订单均摊拆分方式。 */
    public static DispatchArrangementSplitMode fromValue(String value) {
        for (DispatchArrangementSplitMode item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("多订单均摊方式不合法");
    }
}
