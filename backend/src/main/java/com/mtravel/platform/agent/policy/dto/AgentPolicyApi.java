package com.mtravel.platform.agent.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Agent 结构化政策查询协议对象。 */
public final class AgentPolicyApi {

    private AgentPolicyApi() {
    }

    /** 允许 Agent 对外引用的政策白名单字段。 */
    @Schema(name = "AgentPolicyItem")
    public record PolicyItem(
            Long policyId,
            String scope,
            String topic,
            String title,
            String content,
            @Schema(allowableValues = {"auto_answer", "human_review", "prohibited"}) String reviewLevel,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String version,
            OffsetDateTime updatedAt
    ) { }

    /** 同一优先级无法自动消解的政策冲突。 */
    @Schema(name = "AgentPolicyConflict")
    public record PolicyConflict(List<Long> policyIds, String message) { }

    /** 政策聚合结果和最终人工复核要求。 */
    @Schema(name = "AgentPolicySearchResult")
    public record SearchResult(
            boolean answerable,
            boolean mustHandoff,
            @Schema(allowableValues = {"auto_answer", "human_review", "prohibited"})
            String effectiveReviewLevel,
            List<PolicyConflict> conflicts,
            List<PolicyItem> items,
            OffsetDateTime asOf
    ) { }
}
