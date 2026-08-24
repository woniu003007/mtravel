package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import java.math.BigDecimal;

/** 资源供应商绑定下的自费项目报价回显对象。 */
public record ResourceSupplierOptionalItemResponse(
        Long id,
        Long resourceOptionalItemId,
        String projectName,
        BigDecimal costPrice,
        BigDecimal suggestedSalePrice,
        String priceUnit,
        String priceDescription,
        String status
) {
    /** 兼容旧响应构造口径。 */
    public ResourceSupplierOptionalItemResponse(Long id, String projectName, BigDecimal costPrice, String priceUnit, String priceDescription, String status) {
        this(id, null, projectName, costPrice, null, priceUnit, priceDescription, status);
    }
    /** 将自费项目实体转换为资源绑定回显对象。 */
    public static ResourceSupplierOptionalItemResponse fromEntity(PurchaseRelationOptionalItemEntity entity) {
        return new ResourceSupplierOptionalItemResponse(
                entity.getId(),
                entity.getResourceOptionalItemId(),
                entity.getProjectName(),
                entity.getCostPrice(),
                entity.getSuggestedSalePrice(),
                entity.getPriceUnit(),
                entity.getPriceDescription(),
                entity.getStatus()
        );
    }
}
