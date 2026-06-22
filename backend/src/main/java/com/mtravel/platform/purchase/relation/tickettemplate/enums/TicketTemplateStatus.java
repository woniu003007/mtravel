package com.mtravel.platform.purchase.relation.tickettemplate.enums;

import java.util.Arrays;

/**
 * 游客名单模板启停状态。
 */
public enum TicketTemplateStatus {
    /** 模板启用，后续团队景区安排可以使用它导出游客名单。 */
    ACTIVE("active"),

    /** 模板停用，保留配置但不作为默认可用模板。 */
    DISABLED("disabled");

    private final String value;

    TicketTemplateStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 校验接口传入的模板状态是否合法。 */
    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}
