package com.mtravel.platform.sales.booking.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 收客订单价格明细保存请求。
 *
 * @param id 明细 ID，当前版本保存时以订单为单位重建，允许为空。
 * @param lineType 明细类型，例如 adult、child、misc。
 * @param itemName 明细名称。
 * @param unitPrice 单价。
 * @param quantity 数量。
 * @param occupySeat 是否占用团队人数名额。
 * @param remark 业务备注。
 */
public record SalesBookingOrderPriceLineRequest(
        Long id,
        @Size(max = 40, message = "价格类型不能超过40个字符")
        String lineType,
        @Size(max = 120, message = "价格名称不能超过120个字符")
        String itemName,
        @DecimalMin(value = "0.00", message = "单价不能小于0")
        BigDecimal unitPrice,
        @DecimalMin(value = "0.00", message = "数量不能小于0")
        BigDecimal quantity,
        Boolean occupySeat,
        String remark
) {
}
