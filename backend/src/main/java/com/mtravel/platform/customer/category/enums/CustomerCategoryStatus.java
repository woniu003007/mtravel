package com.mtravel.platform.customer.category.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 客户分类状态。
 *
 * <p>数据库字段和前端协议继续使用小写字符串，枚举只负责集中管理允许值，
 * 避免 Service 中散落 active / disabled 字面量。</p>
 */
public enum CustomerCategoryStatus {

    ACTIVE("active"),
    DISABLED("disabled");

    private final String value;

    CustomerCategoryStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CustomerCategoryStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (CustomerCategoryStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("客户分类状态只能是active或disabled");
    }
}
