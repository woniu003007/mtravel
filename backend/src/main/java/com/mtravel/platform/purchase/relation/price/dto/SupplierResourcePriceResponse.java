package com.mtravel.platform.purchase.relation.price.dto;

import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 供应商资源价格返回对象。
 *
 * <p>用于采购关系行内的价格管理列表展示。</p>
 */
@Schema(name = "SupplierResourcePriceResponse", description = "供应商资源价格返回对象")
public record SupplierResourcePriceResponse(
        @Schema(description = "价格明细 ID", example = "3001")
        Long id,

        @Schema(description = "采购关系 ID", example = "1001")
        Long relationId,

        @Schema(description = "费用项目或资源项目 ID", example = "2001")
        Long resourceProjectId,

        @Schema(description = "项目名称，例如门票、房费、车费", example = "成人门票")
        String projectName,

        @Schema(description = "门市价", example = "298.00")
        BigDecimal marketPrice,

        @Schema(description = "同行价", example = "238.00")
        BigDecimal peerPrice,

        @Schema(description = "团队价", example = "218.00")
        BigDecimal teamPrice,

        @Schema(description = "价格说明", example = "平日团队价，节假日另议")
        String priceDescription,

        @Schema(description = "价格状态：active 启用，disabled 停用", example = "active")
        String status,

        @Schema(description = "内部备注", example = "供应商 2026 年报价")
        String remark,

        @Schema(description = "创建人", example = "admin")
        String createdBy,

        @Schema(description = "创建时间", example = "2026-07-09T10:30:00+08:00")
        OffsetDateTime createdAt,

        @Schema(description = "最后修改时间", example = "2026-07-09T11:00:00+08:00")
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
