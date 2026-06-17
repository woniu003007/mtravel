package com.mtravel.platform.sales.product.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 产品国内国际类型枚举。
 *
 * <p>首版只用于产品筛选和展示，不展开出境游签证、护照等专项规则。</p>
 */
public enum SalesProductDomesticType {
    /** 国内产品。 */
    DOMESTIC("domestic"),
    /** 国际产品。 */
    INTERNATIONAL("international");

    private final String value;

    SalesProductDomesticType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 清洗前端传入国内国际标记；为空时默认国内。 */
    public static SalesProductDomesticType fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return DOMESTIC;
        }
        for (SalesProductDomesticType item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("国内国际类型不合法");
    }
}
