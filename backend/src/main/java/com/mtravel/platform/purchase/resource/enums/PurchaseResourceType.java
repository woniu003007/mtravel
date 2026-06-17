package com.mtravel.platform.purchase.resource.enums;

import java.util.Set;

/**
 * 资源总览支持的资源类型。
 *
 * <p>资源总览按当前业务口径只维护景区、酒店、餐厅和购物四类资源。车辆、地接、导游等资料
 * 由独立模块维护，避免资源总览承载过多职责。</p>
 */
public enum PurchaseResourceType {
    /** 景区资源。 */
    SCENIC("scenic"),

    /** 酒店资源。 */
    HOTEL("hotel"),

    /** 餐厅资源。 */
    RESTAURANT("restaurant"),

    /** 购物资源。 */
    SHOPPING("shopping");

    private static final Set<String> VALUES = Set.of("scenic", "hotel", "restaurant", "shopping");

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
