package com.mtravel.platform.purchase.relation.price.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * 供应商资源价格保存请求。
 *
 * @param relationId 采购关系 ID
 * @param resourceProjectId 费用项目 ID
 * @param marketPrice 门市价
 * @param peerPrice 同行价
 * @param teamPrice 团队价
 * @param priceDescription 价格说明
 * @param status 状态，active 启用，disabled 停用
 * @param remark 备注
 */
public record SupplierResourcePriceSaveRequest(
        @NotNull(message = "采购关系不能为空") Long relationId,
        @NotNull(message = "项目类型不能为空") Long resourceProjectId,
        @DecimalMin(value = "0.00", message = "门市价不能小于0") BigDecimal marketPrice,
        @DecimalMin(value = "0.00", message = "同行价不能小于0") BigDecimal peerPrice,
        @DecimalMin(value = "0.00", message = "团队价不能小于0") BigDecimal teamPrice,
        String priceDescription,
        @Pattern(regexp = "active|disabled", message = "价格状态不合法") String status,
        String remark
) {}
