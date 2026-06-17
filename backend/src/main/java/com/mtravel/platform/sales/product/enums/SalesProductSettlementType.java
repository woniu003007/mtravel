package com.mtravel.platform.sales.product.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 产品团队安排费用结算类型枚举。
 *
 * <p>用于产品模板阶段标记费用参考是现结还是挂账，后续团队实际结算仍以财务审核为准。</p>
 */
public enum SalesProductSettlementType {
    /** 现结。 */
    CASH("cash"),
    /** 挂账。 */
    CREDIT("credit");

    private final String value;

    SalesProductSettlementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 清洗结算类型；为空时默认挂账。 */
    public static SalesProductSettlementType fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return CREDIT;
        }
        for (SalesProductSettlementType item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("团队安排结算类型不合法");
    }
}
