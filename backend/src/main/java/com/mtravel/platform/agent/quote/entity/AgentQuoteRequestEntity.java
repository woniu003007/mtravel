package com.mtravel.platform.agent.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Agent 非标准需求询价任务实体，不代表预订、占位或订单。 */
@TableName("agent_quote_requests")
public class AgentQuoteRequestEntity extends TenantSoftDeleteEntity {

    @TableField("request_no") private String requestNo;
    @TableField("service_token_id") private Long serviceTokenId;
    @TableField("idempotency_key") private String idempotencyKey;
    @TableField("request_hash") private String requestHash;
    @TableField("conversation_id") private String conversationId;
    @TableField("customer_id") private Long customerId;
    @TableField("quote_type") private String quoteType;
    @TableField("source_message") private String sourceMessage;
    @TableField("requirements_json") private String requirementsJson;
    @TableField("related_product_id") private Long relatedProductId;
    @TableField("related_schedule_id") private Long relatedScheduleId;
    @TableField("assigned_employee_id") private Long assignedEmployeeId;
    @TableField("assigned_employee_name") private String assignedEmployeeName;
    @TableField("assigned_department_name") private String assignedDepartmentName;
    @TableField("status") private String status;
    @TableField("customer_visible") private Boolean customerVisible;
    @TableField("reply_text") private String replyText;
    @TableField("total_amount") private BigDecimal totalAmount;
    @TableField("currency") private String currency;
    @TableField("valid_until") private OffsetDateTime validUntil;
    @TableField("approved_by") private String approvedBy;
    @TableField("approved_at") private OffsetDateTime approvedAt;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public Long getServiceTokenId() { return serviceTokenId; }
    public void setServiceTokenId(Long serviceTokenId) { this.serviceTokenId = serviceTokenId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getQuoteType() { return quoteType; }
    public void setQuoteType(String quoteType) { this.quoteType = quoteType; }
    public String getSourceMessage() { return sourceMessage; }
    public void setSourceMessage(String sourceMessage) { this.sourceMessage = sourceMessage; }
    public String getRequirementsJson() { return requirementsJson; }
    public void setRequirementsJson(String requirementsJson) { this.requirementsJson = requirementsJson; }
    public Long getRelatedProductId() { return relatedProductId; }
    public void setRelatedProductId(Long relatedProductId) { this.relatedProductId = relatedProductId; }
    public Long getRelatedScheduleId() { return relatedScheduleId; }
    public void setRelatedScheduleId(Long relatedScheduleId) { this.relatedScheduleId = relatedScheduleId; }
    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public void setAssignedEmployeeId(Long assignedEmployeeId) { this.assignedEmployeeId = assignedEmployeeId; }
    public String getAssignedEmployeeName() { return assignedEmployeeName; }
    public void setAssignedEmployeeName(String assignedEmployeeName) { this.assignedEmployeeName = assignedEmployeeName; }
    public String getAssignedDepartmentName() { return assignedDepartmentName; }
    public void setAssignedDepartmentName(String assignedDepartmentName) { this.assignedDepartmentName = assignedDepartmentName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getCustomerVisible() { return customerVisible; }
    public void setCustomerVisible(Boolean customerVisible) { this.customerVisible = customerVisible; }
    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(OffsetDateTime validUntil) { this.validUntil = validUntil; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
}
