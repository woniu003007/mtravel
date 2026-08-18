package com.mtravel.platform.purchase.resource.enums;

import java.util.Set;

/** 景区对客营业状态，与资源主档的启用、停用状态相互独立。 */
public enum ScenicBusinessStatus {
    /** 尚未核实营业状态。 */
    UNMAINTAINED("unmaintained"),
    /** 正常营业。 */
    OPEN("open"),
    /** 暂停营业，后续可能恢复。 */
    SUSPENDED("suspended"),
    /** 已停止营业。 */
    CLOSED("closed");

    private static final Set<String> VALUES = Set.of("unmaintained", "open", "suspended", "closed");

    private final String value;

    ScenicBusinessStatus(String value) {
        this.value = value;
    }

    /** 判断接口入参是否属于允许的营业状态。 */
    public static boolean contains(String value) {
        return VALUES.contains(value);
    }

    /** 返回接口和数据库使用的稳定值。 */
    public String value() {
        return value;
    }
}
