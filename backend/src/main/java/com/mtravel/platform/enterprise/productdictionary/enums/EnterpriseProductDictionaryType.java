package com.mtravel.platform.enterprise.productdictionary.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 产品字典类型。
 *
 * <p>这些类型用于区分产品模板中的不同可配置选项。业务类型来自可维护字典；
 * 接待标准和产品主题虽然老系统是固定项，新系统也放入字典，便于后续按企业习惯调整。</p>
 */
public enum EnterpriseProductDictionaryType {

    /** 产品业务类型，例如疗休养、定制团、地接团。 */
    BUSINESS_TYPE("business_type"),

    /** 接待标准，例如商务/快捷、携程四钻、携程五钻。 */
    RECEPTION_STANDARD("reception_standard"),

    /** 产品主题，例如观光、亲子游、研学、团建。 */
    PRODUCT_THEME("product_theme");

    private final String value;

    EnterpriseProductDictionaryType(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 解析并校验前端传入的字典类型。
     *
     * @param value 前端传入值
     * @return 合法产品字典类型
     * @throws BizException 当类型为空或不在允许范围内时抛出
     */
    public static EnterpriseProductDictionaryType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("产品字典类型不能为空");
        }
        String cleaned = value.trim();
        for (EnterpriseProductDictionaryType type : values()) {
            if (type.value.equals(cleaned)) {
                return type;
            }
        }
        throw new BizException("产品字典类型不合法");
    }
}
