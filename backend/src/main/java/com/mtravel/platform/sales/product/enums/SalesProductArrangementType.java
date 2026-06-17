package com.mtravel.platform.sales.product.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 产品团队安排参数类型枚举。
 *
 * <p>这些类型对应产品模板中可预设的资源和费用项目。正式派房、派车、订票仍由计调和票务模块处理。</p>
 */
public enum SalesProductArrangementType {
    /** 大交通。 */
    TRAFFIC("traffic"),
    /** 住宿。 */
    HOTEL("hotel"),
    /** 用车。 */
    VEHICLE("vehicle"),
    /** 景区门票。 */
    SCENIC("scenic"),
    /** 用餐。 */
    MEAL("meal"),
    /** 其它安排。 */
    OTHER("other"),
    /** 自费项目。 */
    OPTIONAL("optional"),
    /** 购物安排。 */
    SHOPPING("shopping"),
    /** 地接安排。 */
    GROUND_AGENT("ground_agent"),
    /** 附加费用。 */
    EXTRA_FEE("extra_fee");

    private final String value;

    SalesProductArrangementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 清洗团队安排类型，空值或未知值都不允许入库。 */
    public static SalesProductArrangementType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("团队安排类型不能为空");
        }
        for (SalesProductArrangementType item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("团队安排类型不合法");
    }
}
