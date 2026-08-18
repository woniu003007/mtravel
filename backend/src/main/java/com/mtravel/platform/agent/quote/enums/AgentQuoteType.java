package com.mtravel.platform.agent.quote.enums;

import java.util.Arrays;

/** Agent 第一期允许创建的询价任务类型。 */
public enum AgentQuoteType {
    HOTEL_EXTRA_STAY("hotel_extra_stay"),
    HOTEL_CHANGE("hotel_change"),
    VEHICLE("vehicle"),
    CUSTOM_ROUTE("custom_route"),
    EXTRA_ATTRACTION("extra_attraction"),
    SPECIAL_MEAL("special_meal"),
    OTHER("other");

    private final String value;

    AgentQuoteType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 按稳定接口值解析询价类型。 */
    public static AgentQuoteType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
