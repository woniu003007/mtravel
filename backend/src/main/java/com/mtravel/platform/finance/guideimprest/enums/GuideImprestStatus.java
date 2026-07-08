package com.mtravel.platform.finance.guideimprest.enums;

/**
 * 导游备用金申请状态枚举。
 */
public enum GuideImprestStatus {

    /** 草稿，暂未提交审批。 */
    DRAFT("draft"),

    /** 待总经理审批。 */
    PENDING_MANAGER("pending_manager"),

    /** 总经理已同意，允许财务付款。 */
    MANAGER_APPROVED("manager_approved"),

    /** 总经理已拒绝。 */
    MANAGER_REJECTED("manager_rejected"),

    /** 已完成本申请付款。 */
    PAID("paid"),

    /** 已进入导游结算并完成核销。 */
    SETTLED("settled"),

    /** 已取消。 */
    CANCELLED("cancelled");

    private final String value;

    GuideImprestStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
