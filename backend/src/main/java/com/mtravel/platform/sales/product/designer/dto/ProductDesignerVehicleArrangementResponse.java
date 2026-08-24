package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.designer.entity.SalesProductDesignerVehicleArrangementEntity;
import java.math.BigDecimal;

/** 产品级全程用车快照响应。 */
public record ProductDesignerVehicleArrangementResponse(
        Long id,
        Long productId,
        Long resourceId,
        String resourceName,
        Long supplierRelationId,
        Long supplierId,
        String supplierName,
        String priceMode,
        String vehicleType,
        Integer startDayNo,
        Integer endDayNo,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal costAmount,
        Integer sortOrder,
        String remark,
        String procurementStatus
) {
    /** 将已冻结的用车实体转换成前端展示结构。 */
    public static ProductDesignerVehicleArrangementResponse fromEntity(
            SalesProductDesignerVehicleArrangementEntity entity
    ) {
        String priceMode = entity.getPriceModeSnapshot();
        boolean pending = "pending".equals(priceMode)
                || entity.getSupplierRelationIdSnapshot() == null;
        String procurementStatus = "not_required".equals(priceMode)
                ? "not_required"
                : pending ? "pending" : "quoted";
        return new ProductDesignerVehicleArrangementResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getResourceId(),
                entity.getResourceNameSnapshot(),
                entity.getSupplierRelationIdSnapshot(),
                entity.getSupplierId(),
                entity.getSupplierNameSnapshot(),
                priceMode,
                entity.getVehicleTypeSnapshot(),
                entity.getStartDayNo(),
                entity.getEndDayNo(),
                entity.getQuantitySnapshot(),
                entity.getUnitPriceSnapshot(),
                entity.getCostAmountSnapshot(),
                entity.getSortOrder(),
                entity.getRemark(),
                procurementStatus
        );
    }
}
