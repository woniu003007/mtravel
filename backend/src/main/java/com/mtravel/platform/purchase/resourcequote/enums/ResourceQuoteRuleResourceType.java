package com.mtravel.platform.purchase.resourcequote.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 普通资源报价规则支持的资源类型。
 *
 * <p>规则覆盖资源总览之外的车辆、导游、地接和票务等类型，因此不能直接复用仅有四类的
 * {@code PurchaseResourceType} 枚举。</p>
 */
public enum ResourceQuoteRuleResourceType {

    /** 酒店资源。 */
    HOTEL("hotel"),

    /** 景区资源。 */
    SCENIC("scenic"),

    /** 车辆资源。 */
    VEHICLE("vehicle"),

    /** 餐厅资源。 */
    RESTAURANT("restaurant"),

    /** 导游资源。 */
    GUIDE("guide"),

    /** 地接外委资源。 */
    GROUND_AGENT("ground_agent"),

    /** 票务资源。 */
    TICKET("ticket"),

    /** 购物资源。 */
    SHOPPING("shopping"),

    /** 其它普通资源。 */
    OTHER("other");

    private final String value;

    ResourceQuoteRuleResourceType(String value) {
        this.value = value;
    }

    /** 返回接口和数据库使用的稳定资源类型值。 */
    public String getValue() {
        return value;
    }

    /** 将请求资源类型转换为枚举，非法值在保存前直接拒绝。 */
    public static ResourceQuoteRuleResourceType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("资源类型不能为空");
        }
        for (ResourceQuoteRuleResourceType type : values()) {
            if (type.value.equals(value.trim())) {
                return type;
            }
        }
        throw new BizException("资源类型不合法");
    }
}
