package com.mtravel.platform.customer.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 客户授信超额审批步骤快照实体。
 *
 * <p>申请发起时按客户等级配置生成，后续修改等级配置不会改变已发起流程。</p>
 */
@TableName("customer_risk_approval_steps")
public class CustomerRiskApprovalStepEntity {

    /** 审批步骤主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户风控审批申请 ID。 */
    @TableField("request_id")
    private Long requestId;

    /** 审批顺序，从 1 开始。 */
    @TableField("step_order")
    private Integer stepOrder;

    /** 指定审批人的系统用户 ID。 */
    @TableField("approver_user_id")
    private Long approverUserId;

    /** 审批人姓名快照。 */
    @TableField("approver_name")
    private String approverName;

    /** 步骤状态：pending、approved、rejected、cancelled。 */
    @TableField("status")
    private String status;

    /** 处理时间。 */
    @TableField("decided_at")
    private OffsetDateTime decidedAt;

    /** 本步审批意见。 */
    @TableField("decision_remark")
    private String decisionRemark;

    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public Long getApproverUserId() { return approverUserId; }
    public void setApproverUserId(Long approverUserId) { this.approverUserId = approverUserId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
    public String getDecisionRemark() { return decisionRemark; }
    public void setDecisionRemark(String decisionRemark) { this.decisionRemark = decisionRemark; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
