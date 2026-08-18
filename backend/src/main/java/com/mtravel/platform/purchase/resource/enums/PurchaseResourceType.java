package com.mtravel.platform.purchase.resource.enums;

import java.util.Set;

/**
 * 资源总览支持的资源类型。
 *
 * <p>资源总览维护后续产品估价可复用的采购资源主档。导游、自费项目和一次性费用不进入这里，
 * 需要结账的可复用采购对象才作为资源维护。</p>
 */
public enum PurchaseResourceType {
    /** 景区资源。 */
    SCENIC("scenic"),

    /** 酒店资源。 */
    HOTEL("hotel"),

    /** 餐厅资源。 */
    RESTAURANT("restaurant"),

    /** 购物资源。 */
    SHOPPING("shopping"),

    /** 用车资源，代表标准车辆规格，例如 33 座旅游大巴。 */
    VEHICLE("vehicle"),

    /** 大交通服务，代表高铁、飞机、轮船等标准票务规格。 */
    TRAFFIC("traffic"),

    /** 地接资源，代表目的地地接服务套餐或能力。 */
    GROUND_AGENT("ground_agent"),

    /** 其它需要供应商报价和结账的可复用资源。 */
    OTHER("other");

    private static final Set<String> VALUES = Set.of(
            "scenic",
            "hotel",
            "restaurant",
            "shopping",
            "vehicle",
            "traffic",
            "ground_agent",
            "other"
    );

    private final String value;

    PurchaseResourceType(String value) {
        this.value = value;
    }

    /** 判断接口入参是否属于资源总览允许的资源类型。 */
    public static boolean contains(String value) {
        return VALUES.contains(value);
    }

    /** 返回接口和数据库使用的稳定类型值。 */
    public String value() {
        return value;
    }
}
