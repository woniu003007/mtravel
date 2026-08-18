package com.mtravel.platform.customer.risk.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/** 客户授信超额审批列表视图。 */
public enum CustomerRiskApprovalView {
    /** 当前步骤等待我处理的申请。 */
    TO_APPROVE("to_approve"),
    /** 由我发起的申请。 */
    INITIATED("initiated"),
    /** 最终通过后抄送给我的申请。 */
    CC("cc");

    private final String value;

    CustomerRiskApprovalView(String value) {
        this.value = value;
    }

    /** 接口查询值。 */
    public String value() {
        return value;
    }

    /** 解析列表视图，不传时默认查询待我审批。 */
    public static CustomerRiskApprovalView fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return TO_APPROVE;
        }
        for (CustomerRiskApprovalView view : values()) {
            if (view.value.equals(value)) {
                return view;
            }
        }
        throw new BizException("审批列表视图不正确");
    }
}
