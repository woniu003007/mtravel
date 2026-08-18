package com.mtravel.platform.agent.handoff.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/** Agent 转人工待办的来源聊天消息实体。 */
@TableName("agent_handoff_messages")
public class AgentHandoffMessageEntity extends TenantSoftDeleteEntity {

    @TableField("handoff_id") private Long handoffId;
    @TableField("message_id") private String messageId;
    @TableField("sender_name") private String senderName;
    @TableField("sent_at") private OffsetDateTime sentAt;
    @TableField("content") private String content;

    public Long getHandoffId() { return handoffId; }
    public void setHandoffId(Long handoffId) { this.handoffId = handoffId; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
