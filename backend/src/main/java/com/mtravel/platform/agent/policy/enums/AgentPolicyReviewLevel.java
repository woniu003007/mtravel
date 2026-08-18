package com.mtravel.platform.agent.policy.enums;

/** Agent 政策的对外回答复核级别。 */
public enum AgentPolicyReviewLevel {
    AUTO_ANSWER("auto_answer", 1),
    HUMAN_REVIEW("human_review", 2),
    PROHIBITED("prohibited", 3);

    private final String value;
    private final int severity;

    AgentPolicyReviewLevel(String value, int severity) {
        this.value = value;
        this.severity = severity;
    }

    public String value() {
        return value;
    }

    public int severity() {
        return severity;
    }

    /** 未知落库值按需人工复核处理，避免误自动回答。 */
    public static AgentPolicyReviewLevel fromStoredValue(String value) {
        for (AgentPolicyReviewLevel level : values()) {
            if (level.value.equals(value)) return level;
        }
        return HUMAN_REVIEW;
    }
}
