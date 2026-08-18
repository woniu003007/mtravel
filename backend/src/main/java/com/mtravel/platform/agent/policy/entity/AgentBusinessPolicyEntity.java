package com.mtravel.platform.agent.policy.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Agent 对外可查询的结构化业务政策实体。 */
@TableName("agent_business_policies")
public class AgentBusinessPolicyEntity extends TenantSoftDeleteEntity {

    @TableField("scope_type") private String scopeType;
    @TableField("scope_id") private Long scopeId;
    @TableField("topic") private String topic;
    @TableField("title") private String title;
    @TableField("content") private String content;
    @TableField("review_level") private String reviewLevel;
    @TableField("effective_from") private LocalDate effectiveFrom;
    @TableField("effective_to") private LocalDate effectiveTo;
    @TableField("version") private String version;
    @TableField("status") private String status;
    @TableField("approved_by") private String approvedBy;
    @TableField("approved_at") private OffsetDateTime approvedAt;

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReviewLevel() { return reviewLevel; }
    public void setReviewLevel(String reviewLevel) { this.reviewLevel = reviewLevel; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
}
