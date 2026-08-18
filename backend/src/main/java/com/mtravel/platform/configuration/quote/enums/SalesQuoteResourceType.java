package com.mtravel.platform.configuration.quote.enums;

import com.mtravel.platform.common.BizException;
import java.util.Arrays;

/**
 * 销售报价配置支持的普通资源类型。
 */
public enum SalesQuoteResourceType {
    /** 景区资源。 */
    SCENIC("scenic"),
    /** 酒店资源。 */
    HOTEL("hotel"),
    /** 餐饮资源。 */
    RESTAURANT("restaurant"),
    /** 用车资源。 */
    VEHICLE("vehicle"),
    /** 大交通资源。 */
    TRANSPORT("transport"),
    /** 其它资源。 */
    OTHER("other"),
    /** 杂费。 */
    MISC("misc");

    private final String value;

    SalesQuoteResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 校验资源类型。购物不进入销售报价配置，因此不在允许值中。
     */
    public static String requireValid(String value) {
        return Arrays.stream(values())
                .map(SalesQuoteResourceType::getValue)
                .filter(item -> item.equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException("报价资源类型不合法"));
    }
}
