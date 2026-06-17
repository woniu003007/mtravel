package com.mtravel.platform.enterprise.companyinfo.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 企业签约状态。
 *
 * <p>该状态描述企业支付宝或相关线上签约资料是否完成，仅用于企业资料展示和后续支付能力判断。</p>
 */
public enum EnterpriseCompanySignStatus {

    /** 未签约或签约资料未完成。 */
    UNSIGNED("unsigned"),

    /** 已完成签约或资料确认。 */
    SIGNED("signed");

    private final String value;

    EnterpriseCompanySignStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /** 解析前端签约状态，空值默认未签约，非法值直接抛出业务异常。 */
    public static EnterpriseCompanySignStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return UNSIGNED;
        }
        for (EnterpriseCompanySignStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("企业签约状态不合法");
    }
}
