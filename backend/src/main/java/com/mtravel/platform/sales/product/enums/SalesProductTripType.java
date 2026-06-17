package com.mtravel.platform.sales.product.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 产品出团类型枚举。
 *
 * <p>该字段是产品模板上的发团规则，后续团期生成时可作为默认频率参考。</p>
 */
public enum SalesProductTripType {
    /** 每天发。 */
    DAILY("daily"),
    /** 每周发。 */
    WEEKLY("weekly"),
    /** 不定期发。 */
    IRREGULAR("irregular");

    private final String value;

    SalesProductTripType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 清洗前端传入出团类型；为空时按不定期处理。 */
    public static SalesProductTripType fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return IRREGULAR;
        }
        for (SalesProductTripType item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("出团类型不合法");
    }
}
