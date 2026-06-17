package com.mtravel.platform.enterprise.companyinfo.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 企业公司信息状态。
 *
 * <p>公司信息通常只有一份。停用后保留历史资料，但不建议继续作为新合同甲方默认值。</p>
 */
public enum EnterpriseCompanyInfoStatus {

    /** 启用，可作为合同甲方、确认件和企业资料展示默认值。 */
    ACTIVE("active"),

    /** 停用，保留历史记录但不作为新业务默认资料。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseCompanyInfoStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /** 解析前端状态，空值默认启用，非法值直接抛出业务异常。 */
    public static EnterpriseCompanyInfoStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (EnterpriseCompanyInfoStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("公司信息状态不合法");
    }
}
