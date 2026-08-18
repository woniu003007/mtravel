package com.mtravel.platform.agent.handoff.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/** Agent 转人工待办创建协议对象。 */
public final class AgentHandoffApi {

    private AgentHandoffApi() {
    }

    /** 转人工待办创建请求。 */
    @JsonIgnoreProperties(ignoreUnknown = false)
    @Schema(name = "AgentHandoffCreateRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record CreateRequest(
            @NotBlank @Size(max = 100) String conversationId,
            @NotNull @Positive Long customerId,
            @NotBlank @Schema(allowableValues = {
                    "price_required", "policy_review", "inventory_uncertain", "booking_intent",
                    "custom_request", "complaint", "system_error", "other"
            }) String reasonCode,
            @Schema(defaultValue = "normal", allowableValues = {"low", "normal", "high", "urgent"})
            String priority,
            @NotBlank @Size(max = 1000) String summary,
            @NotEmpty @Size(max = 20) List<@Valid SourceMessage> sourceMessages,
            @Valid Related related
    ) { }

    /** 待办保存的来源聊天消息。 */
    @JsonIgnoreProperties(ignoreUnknown = false)
    @Schema(name = "AgentHandoffSourceMessage", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record SourceMessage(
            @NotBlank @Size(max = 100) String messageId,
            @NotBlank @Size(max = 100) String senderName,
            @NotNull OffsetDateTime sentAt,
            @NotBlank @Size(max = 2000) String content
    ) { }

    /** 转人工待办可关联的业务对象，不允许指定负责人。 */
    @JsonIgnoreProperties(ignoreUnknown = false)
    @Schema(name = "AgentHandoffRelated", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record Related(
            @Positive Long productId,
            @Positive Long scheduleId,
            @Size(max = 80) String teamNo,
            @Size(max = 40) String quoteRequestId
    ) { }

    /** 待办分配结果，不包含员工手机、邮箱和账号。 */
    @Schema(name = "AgentHandoffAssignee")
    public record Assignee(Long employeeId, String employeeName, String departmentName) { }

    /** 转人工待办创建成功结果。 */
    @Schema(name = "AgentHandoffCreateResult")
    public record CreateResult(
            String handoffId,
            String status,
            String statusLabel,
            Assignee assignee,
            OffsetDateTime createdAt
    ) { }
}
