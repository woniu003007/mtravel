package com.mtravel.platform.purchase.groundagent.dto;

import com.mtravel.platform.purchase.groundagent.entity.GroundAgentEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 地接外委返回对象。 */
public record GroundAgentResponse(
        Long id, String groundAgentName, String city, String contactName, String contactPhone,
        String taskName, String itineraryRequirement, BigDecimal totalBudget,
        Long confirmationAttachmentId, String confirmationFileUrl, String status, String remark,
        String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static GroundAgentResponse fromEntity(GroundAgentEntity entity) {
        return new GroundAgentResponse(
                entity.getId(), entity.getGroundAgentName(), entity.getCity(), entity.getContactName(), entity.getContactPhone(),
                entity.getTaskName(), entity.getItineraryRequirement(), entity.getTotalBudget(),
                entity.getConfirmationAttachmentId(), entity.getConfirmationFileUrl(), entity.getStatus(), entity.getRemark(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
