package com.mtravel.platform.purchase.resource.optional.dto;

import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;

/** 资源级自费项目回显对象。 */
public record PurchaseResourceOptionalItemResponse(
        Long id, Long resourceId, String projectName, String optionalItemType, String status
) {
    public static PurchaseResourceOptionalItemResponse fromEntity(PurchaseResourceOptionalItemEntity entity) {
        return new PurchaseResourceOptionalItemResponse(entity.getId(), entity.getResourceId(), entity.getProjectName(), entity.getItemType(), entity.getStatus());
    }
}
