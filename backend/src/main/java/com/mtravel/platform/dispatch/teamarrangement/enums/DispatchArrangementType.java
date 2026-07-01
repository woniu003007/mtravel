package com.mtravel.platform.dispatch.teamarrangement.enums;

import com.mtravel.platform.common.BizException;

/**
 * 正式团队安排资源类型。
 *
 * <p>正式团队安排成本按资源类型进入应付、报账、审核和统计。枚举值必须与前端团队安排分类保持一致。</p>
 */
public enum DispatchArrangementType {
    /** 大交通成本。 */
    TRAFFIC("traffic"),
    /** 住宿成本。 */
    HOTEL("hotel"),
    /** 用车成本。 */
    VEHICLE("vehicle"),
    /** 景区门票或景区项目成本。 */
    SCENIC("scenic"),
    /** 用餐成本。 */
    MEAL("meal"),
    /** 其它成本。 */
    OTHER("other"),
    /** 自费项目收入成本。 */
    OPTIONAL("optional"),
    /** 购物消费、返佣或人头费。 */
    SHOPPING("shopping"),
    /** 地接成本。 */
    GROUND_AGENT("ground_agent"),
    /** 附加费用。 */
    EXTRA_FEE("extra_fee");

    private final String value;

    DispatchArrangementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 解析并校验团队安排资源类型。 */
    public static DispatchArrangementType fromValue(String value) {
        for (DispatchArrangementType item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BizException("团队安排类型不合法");
    }
}
