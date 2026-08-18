package com.mtravel.platform.purchase.resource.enums;

import java.util.Arrays;

/** 资源主档的默认采购属性。 */
public enum PurchaseResourceProcurementMode {
    /** 默认需要供应商采购、产生成本或结算。 */
    REQUIRED("required"),
    /** 默认免费或无需采购，仅用于行程、地图和产品方案展示。 */
    NOT_REQUIRED("not_required");

    private final String value;

    PurchaseResourceProcurementMode(String value) {
        this.value = value;
    }

    /** 返回数据库与接口使用的稳定枚举值。 */
    public String value() {
        return value;
    }

    /** 判断传入值是否为允许的采购属性。 */
    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}
