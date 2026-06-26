package com.mtravel.platform.sales.ordertransfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 拼团或转团时针对单个订单和目标团队的备注。
 *
 * @param orderId 来源订单 ID
 * @param targetTeamId 目标团队 ID
 * @param remark 备注内容
 */
public record SalesOrderTransferRemarkRequest(
        @NotNull(message = "订单ID不能为空") Long orderId,
        @NotNull(message = "目标团队ID不能为空") Long targetTeamId,
        @Size(max = 1000, message = "备注不能超过1000个字符") String remark
) {
}
