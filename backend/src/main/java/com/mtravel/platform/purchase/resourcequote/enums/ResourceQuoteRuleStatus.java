package com.mtravel.platform.purchase.resourcequote.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 普通资源报价规则状态。
 *
 * <p>数据库和接口沿用小写字符串，枚举集中维护状态值和默认值。</p>
 */
public enum ResourceQuoteRuleStatus {

    /** 启用状态，可作为资源报价的建议和最低加价口径。 */
    ACTIVE("active"),

    /** 停用状态，保留既有规则但不建议后续报价继续引用。 */
    DISABLED("disabled");

    private final String value;

    ResourceQuoteRuleStatus(String value) {
        this.value = value;
    }

    /** 返回接口和数据库使用的稳定状态值。 */
    public String getValue() {
        return value;
    }

    /** 将请求状态转换为枚举；空值按启用处理。 */
    public static ResourceQuoteRuleStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (ResourceQuoteRuleStatus status : values()) {
            if (status.value.equals(value.trim())) {
                return status;
            }
        }
        throw new BizException("普通资源报价规则状态只能是active或disabled");
    }
}
