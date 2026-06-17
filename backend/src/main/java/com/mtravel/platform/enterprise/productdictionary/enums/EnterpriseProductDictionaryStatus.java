package com.mtravel.platform.enterprise.productdictionary.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 产品字典状态。
 *
 * <p>停用字典不会进入产品模板的常规下拉选择，但历史产品已经保存的值仍可保留。</p>
 */
public enum EnterpriseProductDictionaryStatus {

    /** 启用，可被产品模板和团期计划选择。 */
    ACTIVE("active"),

    /** 停用，保留历史数据但不进入常规选择。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseProductDictionaryStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 解析前端状态值。新增或未传状态时默认启用。
     *
     * @param value 前端传入状态
     * @return 合法状态
     */
    public static EnterpriseProductDictionaryStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        String cleaned = value.trim();
        for (EnterpriseProductDictionaryStatus status : values()) {
            if (status.value.equals(cleaned)) {
                return status;
            }
        }
        throw new BizException("产品字典状态不合法");
    }
}
