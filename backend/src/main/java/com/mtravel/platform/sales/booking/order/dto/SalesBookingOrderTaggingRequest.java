package com.mtravel.platform.sales.booking.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 订单管理页标记状态请求。
 *
 * @param tagging true 表示标记为重点订单，false 表示取消标记
 */
public record SalesBookingOrderTaggingRequest(
        @NotNull(message = "标记状态不能为空")
        Boolean tagging
) {
}
