package com.mtravel.platform.customer.risk.enums;

/**
 * 客户风控审批状态枚举。
 *
 * <p>审批单用于客户合同到期、授信超限时的客户等级指定审批人授权，状态只表达审批流转结果。</p>
 */
public enum CustomerRiskApprovalStatus {
    /** 待审批，申请已提交但未处理。 */
    PENDING("pending"),
    /** 已同意，订单保存可引用该审批单。 */
    APPROVED("approved"),
    /** 已拒绝，订单保存不能引用该审批单。 */
    REJECTED("rejected"),
    /** 已取消，申请人或系统作废后不再生效。 */
    CANCELLED("cancelled");

    private final String value;

    CustomerRiskApprovalStatus(String value) {
        this.value = value;
    }

    /** 数据库存储值。 */
    public String value() {
        return value;
    }
}
