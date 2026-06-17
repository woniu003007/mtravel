package com.mtravel.platform.sales.product.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 销售产品状态枚举。
 *
 * <p>产品模板只区分启用和停用。停售、取消等销售动作属于团期层，不混在产品主档状态中。</p>
 */
public enum SalesProductStatus {
    /** 启用，产品可用于后续创建团期。 */
    ACTIVE("active"),
    /** 停用，产品暂不进入新增团期或销售选择。 */
    DISABLED("disabled");

    private final String value;

    SalesProductStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 清洗前端传入状态；为空时默认启用。 */
    public static SalesProductStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (SalesProductStatus item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("产品状态不合法");
    }
}
