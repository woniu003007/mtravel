package com.mtravel.platform.sales.booking.order.enums;

import java.util.Arrays;

/**
 * 销售收客订单状态枚举。
 *
 * <p>订单状态独立于团队状态。团队可以处于正常、停收、取消；订单本身记录未处理、已确认、
 * 已取消，用于后续应收、收款和团队人数统计。</p>
 */
public enum SalesBookingOrderStatus {

    /** 未处理，订单已录入但尚未确认。 */
    PENDING("pending", "未处理"),

    /** 已确认，订单人数会占用团队实收和余位。 */
    CONFIRMED("confirmed", "已确认"),

    /** 已取消，订单保留记录但不占用团队人数。 */
    CANCELLED("cancelled", "已取消");

    private final String value;
    private final String label;

    SalesBookingOrderStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** 校验前端传入的订单状态是否合法。 */
    public static boolean valid(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }

    /** 将状态值转换为中文展示。 */
    public static String labelOf(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .map(SalesBookingOrderStatus::label)
                .findFirst()
                .orElse(value);
    }
}
