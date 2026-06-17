package com.mtravel.platform.enterprise.bankaccount.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 企业银行账号状态。
 *
 * <p>银行账号属于企业基础资料，停用后不再进入新收付款选择，但历史收付款记录仍可保留引用。</p>
 */
public enum EnterpriseBankAccountStatus {

    /** 启用，可用于收款、付款、打印确认件和员工现金账授权。 */
    ACTIVE("active"),

    /** 停用，保留历史记录但不建议继续选择。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseBankAccountStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 解析前端传入状态。
     *
     * <p>新增时前端可不传状态，此时默认启用；传入非法状态时直接抛业务异常，避免脏数据落库。</p>
     */
    public static EnterpriseBankAccountStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (EnterpriseBankAccountStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("银行账号状态不合法");
    }
}
