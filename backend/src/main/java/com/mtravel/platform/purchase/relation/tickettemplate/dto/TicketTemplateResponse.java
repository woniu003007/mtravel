package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateEntity;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 采购关系游客名单模板返回对象。
 */
public record TicketTemplateResponse(
        Long id,
        Long relationId,
        String templateName,
        Long attachmentId,
        String templateFileUrl,
        String originalFilename,
        String sheetName,
        Integer headerRow,
        Integer dataStartRow,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TicketTemplateFieldResponse> fields
) {
    /** 将模板主表实体和字段映射转换为接口返回对象。 */
    public static TicketTemplateResponse fromEntity(
            PurchaseRelationTicketTemplateEntity entity,
            List<TicketTemplateFieldResponse> fields
    ) {
        return new TicketTemplateResponse(
                entity.getId(),
                entity.getRelationId(),
                entity.getTemplateName(),
                entity.getAttachmentId(),
                entity.getTemplateFileUrl(),
                entity.getOriginalFilename(),
                entity.getSheetName(),
                entity.getHeaderRow(),
                entity.getDataStartRow(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                fields
        );
    }
}
