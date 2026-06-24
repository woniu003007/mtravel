package com.mtravel.platform.sales.team.enums;

/**
 * 销售团队价格状态枚举。
 *
 * <p>当前团期页面主要维护启用价格，保留停用状态用于后续价格行临时下架。</p>
 */
public enum SalesTeamPriceStatus {
    /** 价格行启用，可用于销售收客报价。 */
    ACTIVE("active", "启用"),
    /** 价格行停用，保留历史但不参与后续报价选择。 */
    DISABLED("disabled", "停用");

    private final String value;
    private final String label;

    SalesTeamPriceStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
