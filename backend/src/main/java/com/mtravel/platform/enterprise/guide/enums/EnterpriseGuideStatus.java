package com.mtravel.platform.enterprise.guide.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 企业导游档案状态枚举。
 *
 * <p>导游停用后不再进入团队安排和导游端选择，但历史排团、报账和结算记录仍保留引用。</p>
 */
public enum EnterpriseGuideStatus {

    /** 启用状态，可参与团队安排、导游端报账和后续导游结算。 */
    ACTIVE("active"),

    /** 停用状态，保留历史引用，但不再作为新团队可选导游。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseGuideStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 将前端传入状态转换为枚举。
     *
     * @param value 前端传入状态，空值按启用处理
     * @return 合法导游状态
     */
    public static EnterpriseGuideStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (EnterpriseGuideStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("导游状态不合法");
    }
}
