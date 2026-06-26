package com.mtravel.platform.customer.risk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 客户风控审批申请实体，对应 customer_risk_approval_requests 表。
 *
 * <p>本表保存客户合同到期、授信超限时的审批流水和申请时快照。订单、收款、成本不会写入本表，
 * 订单保存只引用已审批通过的申请单作为授权依据。</p>
 */
@TableName("customer_risk_approval_requests")
public class CustomerRiskApprovalRequestEntity extends TenantSoftDeleteEntity {

    /** 客户单位 ID。 */
    @TableField("customer_id")
    private Long customerId;

    /** 客户单位名称快照。 */
    @TableField("customer_name")
    private String customerName;

    /** 团队 ID，可为空。 */
    @TableField("team_id")
    private Long teamId;

    /** 订单 ID，新订单申请时可为空，保存成功后可回填。 */
    @TableField("order_id")
    private Long orderId;

    /** 审批申请编号，同一租户内唯一。 */
    @TableField("request_no")
    private String requestNo;

    /** 本次订单预计应收金额。 */
    @TableField("requested_amount")
    private BigDecimal requestedAmount;

    /** 风险类型，多个类型用英文逗号分隔。 */
    @TableField("risk_types")
    private String riskTypes;

    /** 风险摘要，面向审批人展示。 */
    @TableField("risk_summary")
    private String riskSummary;

    /** 合同有效期止快照。 */
    @TableField("contract_expire_date")
    private LocalDate contractExpireDate;

    /** 授信额度快照。 */
    @TableField("credit_limit")
    private BigDecimal creditLimit;

    /** 已占用额度快照。 */
    @TableField("occupied_amount")
    private BigDecimal occupiedAmount;

    /** 审批中额度快照。 */
    @TableField("pending_approval_amount")
    private BigDecimal pendingApprovalAmount;

    /** 可用额度快照。 */
    @TableField("available_amount")
    private BigDecimal availableAmount;

    /** 超限金额快照。 */
    @TableField("over_limit_amount")
    private BigDecimal overLimitAmount;

    /** 审批状态：pending、approved、rejected、cancelled。 */
    @TableField("status")
    private String status;

    /** 申请人。 */
    @TableField("applicant")
    private String applicant;

    /** 同意审批人。 */
    @TableField("approved_by")
    private String approvedBy;

    /** 同意审批时间。 */
    @TableField("approved_at")
    private OffsetDateTime approvedAt;

    /** 拒绝审批人。 */
    @TableField("rejected_by")
    private String rejectedBy;

    /** 拒绝审批时间。 */
    @TableField("rejected_at")
    private OffsetDateTime rejectedAt;

    /** 审批意见。 */
    @TableField("approval_remark")
    private String approvalRemark;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public String getRiskTypes() { return riskTypes; }
    public void setRiskTypes(String riskTypes) { this.riskTypes = riskTypes; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public LocalDate getContractExpireDate() { return contractExpireDate; }
    public void setContractExpireDate(LocalDate contractExpireDate) { this.contractExpireDate = contractExpireDate; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public BigDecimal getOccupiedAmount() { return occupiedAmount; }
    public void setOccupiedAmount(BigDecimal occupiedAmount) { this.occupiedAmount = occupiedAmount; }
    public BigDecimal getPendingApprovalAmount() { return pendingApprovalAmount; }
    public void setPendingApprovalAmount(BigDecimal pendingApprovalAmount) { this.pendingApprovalAmount = pendingApprovalAmount; }
    public BigDecimal getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(BigDecimal availableAmount) { this.availableAmount = availableAmount; }
    public BigDecimal getOverLimitAmount() { return overLimitAmount; }
    public void setOverLimitAmount(BigDecimal overLimitAmount) { this.overLimitAmount = overLimitAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }
    public OffsetDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(OffsetDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public String getApprovalRemark() { return approvalRemark; }
    public void setApprovalRemark(String approvalRemark) { this.approvalRemark = approvalRemark; }
}
