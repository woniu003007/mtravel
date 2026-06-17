package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import java.time.OffsetDateTime;

/**
 * 资源已绑定供应商返回对象。
 *
 * <p>用于资源总览点击“已绑定 X 家”时查看绑定关系。具体价格明细从采购关系页的价格管理进入。</p>
 */
public record PurchaseResourceBindingResponse(
        Long relationId,
        Long supplierId,
        String supplierName,
        Integer groupQuantity,
        String status,
        OffsetDateTime createdAt
) {
    /** 将采购关系和供应商名称组装为资源绑定行。 */
    public static PurchaseResourceBindingResponse fromEntity(PurchaseRelationEntity entity, String supplierName) {
        return new PurchaseResourceBindingResponse(
                entity.getId(),
                entity.getSupplierId(),
                supplierName,
                entity.getGroupQuantity(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
