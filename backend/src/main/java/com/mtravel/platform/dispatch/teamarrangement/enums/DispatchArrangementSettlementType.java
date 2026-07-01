package com.mtravel.platform.dispatch.teamarrangement.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 正式团队安排默认结算类型。
 *
 * <p>该枚举只描述团队安排成本录入时的默认结算口径，真实付款仍以后续付款和财务审核链路为准。</p>
 */
public enum DispatchArrangementSettlementType {
    /** 现结。 */
    CASH("cash"),
    /** 挂账。 */
    CREDIT("credit");

    private final String value;

    DispatchArrangementSettlementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 解析结算类型；为空时默认挂账。 */
    public static DispatchArrangementSettlementType fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return CREDIT;
        }
        for (DispatchArrangementSettlementType item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("团队安排结算类型不合法");
    }
}
