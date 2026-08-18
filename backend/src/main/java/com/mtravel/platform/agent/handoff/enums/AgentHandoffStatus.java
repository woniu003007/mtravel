package com.mtravel.platform.agent.handoff.enums;

/** Agent 转人工待办对外状态。 */
public enum AgentHandoffStatus {
    OPEN("open", "待处理"),
    PROCESSING("processing", "处理中"),
    RESOLVED("resolved", "已解决"),
    CANCELLED("cancelled", "已取消");

    private final String value;
    private final String label;

    AgentHandoffStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() { return value; }
    public String label() { return label; }

    public static AgentHandoffStatus fromStoredValue(String value) {
        for (AgentHandoffStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        return OPEN;
    }
}
