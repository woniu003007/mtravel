package com.mtravel.platform.configuration.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 销售报价低价审批人员配置实体。
 */
@TableName(value = "sales_quote_approval_members", excludeProperty = "remark")
public class SalesQuoteApprovalMemberEntity extends TenantSoftDeleteEntity {

    /** 人员类型：approver 审批人，cc 抄送人。 */
    @TableField("member_type")
    private String memberType;

    /** 系统用户 ID。 */
    @TableField("system_user_id")
    private Long systemUserId;

    /** 审批顺序，审批人从 1 开始。 */
    @TableField("step_order")
    private Integer stepOrder;

    public String getMemberType() { return memberType; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public Long getSystemUserId() { return systemUserId; }
    public void setSystemUserId(Long systemUserId) { this.systemUserId = systemUserId; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
}
