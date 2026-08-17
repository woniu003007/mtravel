package com.mtravel.platform.customer.creditrule.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 客户授信规则状态。
 *
 * <p>接口和数据库继续使用小写字符串，枚举集中维护允许值，避免业务服务散落状态字面量。</p>
 */
public enum CustomerCreditRuleStatus {

    /** 启用状态，当前客户等级的默认授信规则可被业务流程引用。 */
    ACTIVE("active"),

    /** 停用状态，保留历史配置但不用于后续业务。 */
    DISABLED("disabled");

    private final String value;

    CustomerCreditRuleStatus(String value) {
        this.value = value;
    }

    /** 返回接口和数据库使用的稳定状态值。 */
    public String getValue() {
        return value;
    }

    /**
     * 将请求状态转换为枚举；空值按启用处理。
     *
     * @param value 前端传入的状态
     * @return 合法状态
     */
    public static CustomerCreditRuleStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (CustomerCreditRuleStatus status : values()) {
            if (status.value.equals(value.trim())) {
                return status;
            }
        }
        throw new BizException("客户授信规则状态只能是active或disabled");
    }
}
