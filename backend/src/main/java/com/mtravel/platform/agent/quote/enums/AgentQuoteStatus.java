package com.mtravel.platform.agent.quote.enums;

/** Agent 询价任务对外状态和中文标签。 */
public enum AgentQuoteStatus {
    PENDING("pending", "待处理"),
    PROCESSING("processing", "处理中"),
    QUOTED("quoted", "已报价"),
    REJECTED("rejected", "已拒绝"),
    CANCELLED("cancelled", "已取消"),
    EXPIRED("expired", "已过期");

    private final String value;
    private final String label;

    AgentQuoteStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() { return value; }
    public String label() { return label; }

    /** 未知落库状态按待处理对外展示，不暴露内部值。 */
    public static AgentQuoteStatus fromStoredValue(String value) {
        for (AgentQuoteStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        return PENDING;
    }
}
