package com.mtravel.platform.configuration.quote.enums;

/**
 * 报价配置启停状态。
 */
public enum QuoteConfigStatus {
    /** 启用，参与后续报价规则匹配。 */
    ACTIVE("active"),
    /** 停用，保留历史配置但不再用于新报价。 */
    DISABLED("disabled");

    private final String value;

    QuoteConfigStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将接口传入状态转换为枚举，空值默认启用。
     */
    public static QuoteConfigStatus fromValueOrDefault(String value) {
        if (DISABLED.value.equals(value)) {
            return DISABLED;
        }
        return ACTIVE;
    }
}
