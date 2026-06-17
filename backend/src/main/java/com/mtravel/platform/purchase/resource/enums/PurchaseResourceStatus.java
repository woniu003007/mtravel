package com.mtravel.platform.purchase.resource.enums;

/**
 * 采购资源启停状态。
 *
 * <p>状态只表达资源主档是否可用，不表达采购关系是否有效，也不表达合同是否到期。</p>
 */
public enum PurchaseResourceStatus {
    /** 启用，资源可在查询和绑定供应商时使用。 */
    ACTIVE("active"),

    /** 停用，资源保留台账但不建议继续用于新增业务。 */
    DISABLED("disabled");

    private final String value;

    PurchaseResourceStatus(String value) {
        this.value = value;
    }

    /** 返回接口和数据库使用的稳定状态值。 */
    public String value() {
        return value;
    }
}
