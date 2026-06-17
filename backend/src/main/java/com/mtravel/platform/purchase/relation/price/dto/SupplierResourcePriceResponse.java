package com.mtravel.platform.purchase.relation.price.dto;

import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 供应商资源价格返回对象。
 *
 * <p>用于采购关系行内的价格管理列表展示。</p>
 */
public record SupplierResourcePriceResponse(
        Long id,
        Long relationId,
        Long resourceProjectId,
        String projectName,
        BigDecimal marketPrice,
        BigDecimal peerPrice,
        BigDecimal teamPrice,
        String priceDescription,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将价格实体转换为接口返回对象。 */
    public static SupplierResourcePriceResponse fromEntity(SupplierResourcePriceEntity entity) {
        return new SupplierResourcePriceResponse(
                entity.getId(),
                entity.getRelationId(),
                entity.getResourceProjectId(),
                entity.getProjectName(),
                entity.getMarketPrice(),
                entity.getPeerPrice(),
                entity.getTeamPrice(),
                entity.getPriceDescription(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
