package com.mtravel.platform.sales.booking.order.dto;

import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderPriceLineEntity;
import java.math.BigDecimal;

/**
 * 收客订单价格明细返回对象。
 */
public record SalesBookingOrderPriceLineResponse(
        Long id,
        String lineType,
        String itemName,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal subtotalAmount,
        String remark
) {
    /** 将价格明细实体转换为接口返回对象。 */
    public static SalesBookingOrderPriceLineResponse fromEntity(SalesBookingOrderPriceLineEntity entity) {
        return new SalesBookingOrderPriceLineResponse(
                entity.getId(),
                entity.getLineType(),
                entity.getItemName(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getSubtotalAmount(),
                entity.getRemark()
        );
    }
}
