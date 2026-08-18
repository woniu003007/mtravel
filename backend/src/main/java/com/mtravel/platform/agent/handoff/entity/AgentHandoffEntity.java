package com.mtravel.platform.agent.handoff.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/** Agent 创建的转人工待办实体。 */
@TableName("agent_handoffs")
public class AgentHandoffEntity extends TenantSoftDeleteEntity {

    @TableField("handoff_no") private String handoffNo;
    @TableField("service_token_id") private Long serviceTokenId;
    @TableField("idempotency_key") private String idempotencyKey;
    @TableField("request_hash") private String requestHash;
    @TableField("conversation_id") private String conversationId;
    @TableField("customer_id") private Long customerId;
    @TableField("reason_code") private String reasonCode;
    @TableField("priority") private String priority;
    @TableField("summary") private String summary;
    @TableField("related_product_id") private Long relatedProductId;
    @TableField("related_schedule_id") private Long relatedScheduleId;
    @TableField("related_team_no") private String relatedTeamNo;
    @TableField("related_quote_request_no") private String relatedQuoteRequestNo;
    @TableField("assigned_employee_id") private Long assignedEmployeeId;
    @TableField("assigned_employee_name") private String assignedEmployeeName;
    @TableField("assigned_department_name") private String assignedDepartmentName;
    @TableField("status") private String status;
    @TableField("resolution") private String resolution;
    @TableField("resolved_by") private String resolvedBy;
    @TableField("resolved_at") private OffsetDateTime resolvedAt;

    public String getHandoffNo() { return handoffNo; }
    public void setHandoffNo(String handoffNo) { this.handoffNo = handoffNo; }
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
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getRelatedProductId() { return relatedProductId; }
    public void setRelatedProductId(Long relatedProductId) { this.relatedProductId = relatedProductId; }
    public Long getRelatedScheduleId() { return relatedScheduleId; }
    public void setRelatedScheduleId(Long relatedScheduleId) { this.relatedScheduleId = relatedScheduleId; }
    public String getRelatedTeamNo() { return relatedTeamNo; }
    public void setRelatedTeamNo(String relatedTeamNo) { this.relatedTeamNo = relatedTeamNo; }
    public String getRelatedQuoteRequestNo() { return relatedQuoteRequestNo; }
    public void setRelatedQuoteRequestNo(String value) { this.relatedQuoteRequestNo = value; }
    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public void setAssignedEmployeeId(Long assignedEmployeeId) { this.assignedEmployeeId = assignedEmployeeId; }
    public String getAssignedEmployeeName() { return assignedEmployeeName; }
    public void setAssignedEmployeeName(String assignedEmployeeName) { this.assignedEmployeeName = assignedEmployeeName; }
    public String getAssignedDepartmentName() { return assignedDepartmentName; }
    public void setAssignedDepartmentName(String value) { this.assignedDepartmentName = value; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
