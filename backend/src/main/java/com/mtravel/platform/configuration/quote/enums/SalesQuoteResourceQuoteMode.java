package com.mtravel.platform.configuration.quote.enums;

import com.mtravel.platform.common.BizException;
import java.util.Arrays;

/**
 * 普通资源报价规则允许使用的报价方式。
 */
public enum SalesQuoteResourceQuoteMode {
    /** 仅可按采购成本比例上浮报价。 */
    RATE("rate"),
    /** 仅可在采购成本上固定加价报价。 */
    FIXED("fixed"),
    /** 比例上浮和固定加价两种方式均可报价。 */
    BOTH("both");

    private final String value;

    SalesQuoteResourceQuoteMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 校验报价方式；历史接口未传时默认兼容为两种方式均可报价。
     */
    public static String fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return BOTH.value;
        }
        return Arrays.stream(values())
                .map(SalesQuoteResourceQuoteMode::getValue)
                .filter(value::equals)
                .findFirst()
                .orElseThrow(() -> new BizException("报价方式不合法"));
    }
}
