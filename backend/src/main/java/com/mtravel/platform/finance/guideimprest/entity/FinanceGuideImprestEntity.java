package com.mtravel.platform.finance.guideimprest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 导游备用金申请主表实体。
 *
 * <p>该表保存备用金申请、计算结果快照、总经理审批状态和财务付款汇总。
 * 它不直接代表团队成本，也不直接代表银行流水。</p>
 */
@TableName("finance_guide_imprests")
public class FinanceGuideImprestEntity extends TenantSoftDeleteEntity {

    /** 备用金申请编号。 */
    @TableField("request_no")
    private String requestNo;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队类型快照。 */
    @TableField("team_type")
    private String teamType;

    /** 业务类型快照。 */
    @TableField("business_type")
    private String businessType;

    /** 发团日期快照。 */
    @TableField("departure_date")
    private LocalDate departureDate;

    /** 部门 ID 快照。 */
    @TableField("department_id")
    private Long departmentId;

    /** 部门名称快照。 */
    @TableField("department_name")
    private String departmentName;

    /** 操作计调员工 ID 快照。 */
    @TableField("operator_employee_id")
    private Long operatorEmployeeId;

    /** 操作计调姓名快照。 */
    @TableField("operator_employee_name")
    private String operatorEmployeeName;

    /** 导游档案 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 导游手机号快照。 */
    @TableField("guide_mobile")
    private String guideMobile;

    /** 团队实收人数快照。 */
    @TableField("guest_count")
    private Integer guestCount;

    /** 公司规定加点率，按百分数保存，例如 70 表示 70%。 */
    @TableField("company_markup_rate")
    private BigDecimal companyMarkupRate;

    /** 计算时的现付总成本。 */
    @TableField("cash_cost_amount")
    private BigDecimal cashCostAmount;

    /** 计算时的自费加点抵扣金额。 */
    @TableField("optional_deduction_amount")
    private BigDecimal optionalDeductionAmount;

    /** 原始公式计算结果，可为负数。 */
    @TableField("calculated_amount")
    private BigDecimal calculatedAmount;

    /** 建议发放备用金金额，负数时为 0。 */
    @TableField("suggested_imprest_amount")
    private BigDecimal suggestedImprestAmount;

    /** 公式为负数时导游应上交金额。 */
    @TableField("guide_turn_in_amount")
    private BigDecimal guideTurnInAmount;

    /** 本次申请发放金额。 */
    @TableField("requested_amount")
    private BigDecimal requestedAmount;

    /** 总经理同意后的审批金额。 */
    @TableField("approved_amount")
    private BigDecimal approvedAmount;

    /** 已付款金额汇总。 */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /** 剩余未付款金额。 */
    @TableField("balance_amount")
    private BigDecimal balanceAmount;

    /** 申请状态。 */
    @TableField("status")
    private String status;

    /** 申请人账号或姓名。 */
    @TableField("applicant")
    private String applicant;

    /** 申请提交时间。 */
    @TableField("applied_at")
    private OffsetDateTime appliedAt;

    /** 审批同意人。 */
    @TableField("approved_by")
    private String approvedBy;

    /** 审批同意时间。 */
    @TableField("approved_at")
    private OffsetDateTime approvedAt;

    /** 审批拒绝人。 */
    @TableField("rejected_by")
    private String rejectedBy;

    /** 审批拒绝时间。 */
    @TableField("rejected_at")
    private OffsetDateTime rejectedAt;

    /** 审批意见。 */
    @TableField("approval_remark")
    private String approvalRemark;

    /** 作废人。 */
    @TableField("cancelled_by")
    private String cancelledBy;

    /** 作废时间。 */
    @TableField("cancelled_at")
    private OffsetDateTime cancelledAt;

    /** 作废原因。 */
    @TableField("cancel_reason")
    private String cancelReason;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getOperatorEmployeeId() { return operatorEmployeeId; }
    public void setOperatorEmployeeId(Long operatorEmployeeId) { this.operatorEmployeeId = operatorEmployeeId; }
    public String getOperatorEmployeeName() { return operatorEmployeeName; }
    public void setOperatorEmployeeName(String operatorEmployeeName) { this.operatorEmployeeName = operatorEmployeeName; }
    public Long getGuideId() { return guideId; }
    public void setGuideId(Long guideId) { this.guideId = guideId; }
    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }
    public String getGuideMobile() { return guideMobile; }
    public void setGuideMobile(String guideMobile) { this.guideMobile = guideMobile; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public BigDecimal getCompanyMarkupRate() { return companyMarkupRate; }
    public void setCompanyMarkupRate(BigDecimal companyMarkupRate) { this.companyMarkupRate = companyMarkupRate; }
    public BigDecimal getCashCostAmount() { return cashCostAmount; }
    public void setCashCostAmount(BigDecimal cashCostAmount) { this.cashCostAmount = cashCostAmount; }
    public BigDecimal getOptionalDeductionAmount() { return optionalDeductionAmount; }
    public void setOptionalDeductionAmount(BigDecimal optionalDeductionAmount) { this.optionalDeductionAmount = optionalDeductionAmount; }
    public BigDecimal getCalculatedAmount() { return calculatedAmount; }
    public void setCalculatedAmount(BigDecimal calculatedAmount) { this.calculatedAmount = calculatedAmount; }
    public BigDecimal getSuggestedImprestAmount() { return suggestedImprestAmount; }
    public void setSuggestedImprestAmount(BigDecimal suggestedImprestAmount) { this.suggestedImprestAmount = suggestedImprestAmount; }
    public BigDecimal getGuideTurnInAmount() { return guideTurnInAmount; }
    public void setGuideTurnInAmount(BigDecimal guideTurnInAmount) { this.guideTurnInAmount = guideTurnInAmount; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }
    public OffsetDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(OffsetDateTime appliedAt) { this.appliedAt = appliedAt; }
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
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
