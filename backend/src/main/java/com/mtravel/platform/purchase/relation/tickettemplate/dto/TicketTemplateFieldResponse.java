package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateFieldEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TicketTemplateFillMode;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TouristSystemField;

/**
 * 游客名单模板字段映射返回对象。
 */
public record TicketTemplateFieldResponse(
        Long id,
        Long templateId,
        String templateHeader,
        Integer columnIndex,
        String systemField,
        String systemFieldLabel,
        String fillMode,
        String fillModeLabel,
        String fixedValue,
        Boolean required,
        Integer sortOrder
) {
    /** 将字段映射实体转换为接口返回对象。 */
    public static TicketTemplateFieldResponse fromEntity(PurchaseRelationTicketTemplateFieldEntity entity) {
        String label = TouristSystemField.fromValue(entity.getSystemField())
                .map(TouristSystemField::label)
                .orElse(entity.getSystemField());
        String mode = TicketTemplateFillMode.fromValue(entity.getFillMode())
                .map(TicketTemplateFillMode::value)
                .orElse(TicketTemplateFillMode.TOURIST_FIELD.value());
        String modeLabel = TicketTemplateFillMode.fromValue(mode)
                .map(TicketTemplateFillMode::label)
                .orElse(mode);
        return new TicketTemplateFieldResponse(
                entity.getId(),
                entity.getTemplateId(),
                entity.getTemplateHeader(),
                entity.getColumnIndex(),
                entity.getSystemField(),
                label,
                mode,
                modeLabel,
                entity.getFixedValue(),
                Boolean.TRUE.equals(entity.getRequired()),
                entity.getSortOrder()
        );
    }
}
