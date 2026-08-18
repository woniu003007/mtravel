package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import java.math.BigDecimal;

/** 资源供应商绑定下的报价明细，用于资源页编辑回显。 */
public record ResourceSupplierPriceLineResponse(
        Long resourceProjectId,
        String projectName,
        BigDecimal teamPrice,
        String priceDescription
) {
    /** 将价格实体转换为资源页报价明细。 */
    public static ResourceSupplierPriceLineResponse fromEntity(SupplierResourcePriceEntity entity) {
        return new ResourceSupplierPriceLineResponse(
                entity.getResourceProjectId(),
                entity.getProjectName(),
                entity.getTeamPrice(),
                entity.getPriceDescription()
        );
    }
}
