package com.mtravel.platform.sales.ordertransfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 单个“来源订单 x 目标团队”拼团确认项。
 *
 * <p>老系统拼团确认页允许每个组合单独填写拼团备注、拼团单价和价格类型；
 * 后端按本对象生成目标团拼团子订单的价格明细，不复制来源订单原价格。</p>
 *
 * @param orderId 来源订单 ID
 * @param targetTeamId 目标团队 ID
 * @param remark 拼团备注
 * @param unitPrice 拼团单价，默认 0
 * @param priceType 价格类型，默认成人
 */
public record SalesOrderTransferMergeItemRequest(
        @NotNull(message = "订单ID不能为空") Long orderId,
        @NotNull(message = "目标团队ID不能为空") Long targetTeamId,
        @Size(max = 1000, message = "拼团备注不能超过1000个字符") String remark,
        @DecimalMin(value = "0.00", message = "拼团单价不能小于0") BigDecimal unitPrice,
        @Size(max = 30, message = "价格类型不能超过30个字符") String priceType
) {
}
