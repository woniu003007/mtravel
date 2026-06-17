package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import java.math.BigDecimal;

/**
 * 销售产品团队安排参数返回对象。
 *
 * <p>用于产品编辑页团队安排 tab 回显默认安排和费用参考。</p>
 */
public record SalesProductArrangementItemResponse(
        Long id,
        String arrangementType,
        String itemName,
        String arrangementContent,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String unitName,
        String settlementType,
        String remark
) {
    /** 将团队安排参数实体转换为接口响应。 */
    public static SalesProductArrangementItemResponse fromEntity(SalesProductArrangementItemEntity entity) {
        return new SalesProductArrangementItemResponse(
                entity.getId(),
                entity.getArrangementType(),
                entity.getItemName(),
                entity.getArrangementContent(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getUnitName(),
                entity.getSettlementType(),
                entity.getRemark()
        );
    }
}
