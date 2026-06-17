package com.mtravel.platform.customer.unit.enums;

import com.mtravel.platform.common.BizException;

/**
 * 客户结款方式枚举。
 *
 * <p>该枚举用于客户单位主档，表达客户应收款结算账期。订单和收款模块后续根据该值、
 * 账单起始日期和结款日计算账期提醒，避免把结款方式作为自由文本散落在业务代码中。</p>
 */
public enum CustomerSettlementMethod {

    /** 不限，表示客户暂未配置固定结款规则。 */
    UNLIMITED("unlimited"),

    /** 现结，表示订单或团队业务发生后原则上当期结清。 */
    CASH("cash"),

    /** 1 个月账期。 */
    MONTHLY_1("monthly_1"),

    /** 2 个月账期。 */
    MONTHLY_2("monthly_2"),

    /** 3 个月账期。 */
    MONTHLY_3("monthly_3"),

    /** 4 个月账期。 */
    MONTHLY_4("monthly_4"),

    /** 5 个月账期。 */
    MONTHLY_5("monthly_5"),

    /** 6 个月账期。 */
    MONTHLY_6("monthly_6"),

    /** 7 个月账期。 */
    MONTHLY_7("monthly_7"),

    /** 8 个月账期。 */
    MONTHLY_8("monthly_8"),

    /** 9 个月账期。 */
    MONTHLY_9("monthly_9"),

    /** 10 个月账期。 */
    MONTHLY_10("monthly_10"),

    /** 11 个月账期。 */
    MONTHLY_11("monthly_11"),

    /** 12 个月账期。 */
    MONTHLY_12("monthly_12");

    private final String value;

    CustomerSettlementMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 解析前端传入的结款方式。为空时使用不限，非法值直接返回业务异常。
     */
    public static CustomerSettlementMethod fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return UNLIMITED;
        }
        for (CustomerSettlementMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new BizException("客户结款方式不合法");
    }
}
