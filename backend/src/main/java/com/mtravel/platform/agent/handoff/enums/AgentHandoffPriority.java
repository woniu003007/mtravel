package com.mtravel.platform.agent.handoff.enums;

/** Agent 转人工待办优先级。 */
public enum AgentHandoffPriority {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    URGENT("urgent");

    private final String value;

    AgentHandoffPriority(String value) {
        this.value = value;
    }

    public String value() { return value; }

    /** 空值按 normal 处理，非法值返回 null 交由服务校验。 */
    public static AgentHandoffPriority fromValue(String value) {
        if (value == null || value.isBlank()) return NORMAL;
        for (AgentHandoffPriority priority : values()) {
            if (priority.value.equals(value)) return priority;
        }
        return null;
    }
}
