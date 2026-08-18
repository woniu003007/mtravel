package com.mtravel.platform.agent.policy.enums;

import java.util.Arrays;

/** Agent 可查询的结构化政策主题。 */
public enum AgentPolicyTopic {
    MINOR_WITHOUT_GUARDIAN("minor_without_guardian"),
    GUARDIAN_CONSENT("guardian_consent"),
    LIABILITY_WAIVER("liability_waiver"),
    AGE_RESTRICTION("age_restriction"),
    HEALTH_REQUIREMENT("health_requirement"),
    SPECIAL_DIET("special_diet"),
    INSURANCE("insurance"),
    REFUND_CHANGE("refund_change"),
    ATTRACTION_TRANSPORT("attraction_transport"),
    OPTIONAL_ITEMS("optional_items"),
    SHOPPING("shopping");

    private final String value;

    AgentPolicyTopic(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 判断请求值是否为第一期支持的政策主题。 */
    public static boolean supports(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}
