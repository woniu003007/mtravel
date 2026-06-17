package com.mtravel.platform.customer.unit.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 客户单位主档状态。
 *
 * <p>这里只表达客户资料是否启用。合同未签、合同到期、授信超限等业务状态，
 * 应由合同、授信或应收数据计算，不能混入客户主档状态。</p>
 */
public enum CustomerUnitStatus {

    ACTIVE("active"),
    DISABLED("disabled");

    private final String value;

    CustomerUnitStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CustomerUnitStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (CustomerUnitStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("客户状态只能是active或disabled");
    }
}
