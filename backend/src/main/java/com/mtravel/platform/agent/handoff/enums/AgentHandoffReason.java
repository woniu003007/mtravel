package com.mtravel.platform.agent.handoff.enums;

/** Agent 转人工待办原因的稳定接口枚举。 */
public enum AgentHandoffReason {
    PRICE_REQUIRED("price_required"),
    POLICY_REVIEW("policy_review"),
    INVENTORY_UNCERTAIN("inventory_uncertain"),
    BOOKING_INTENT("booking_intent"),
    CUSTOM_REQUEST("custom_request"),
    COMPLAINT("complaint"),
    SYSTEM_ERROR("system_error"),
    OTHER("other");

    private final String value;

    AgentHandoffReason(String value) {
        this.value = value;
    }

    public String value() { return value; }

    /** 按对外稳定值解析转人工原因。 */
    public static AgentHandoffReason fromValue(String value) {
        for (AgentHandoffReason reason : values()) {
            if (reason.value.equals(value)) return reason;
        }
        return null;
    }
}
