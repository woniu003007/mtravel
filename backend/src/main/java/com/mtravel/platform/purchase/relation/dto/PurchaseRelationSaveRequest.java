package com.mtravel.platform.purchase.relation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 采购关系保存请求。
 *
 * @param resourceId 绑定资源 ID
 * @param supplierId 供应商 ID
 * @param groupQuantity 成团数量，0 表示散团同价
 * @param isDefault 是否设为当前资源默认供应商
 * @param status 状态，active 有效，disabled 停用，expired 兼容历史状态
 * @param remark 备注
 */
public record PurchaseRelationSaveRequest(
        @NotNull(message = "绑定资源不能为空") Long resourceId,
        @NotNull(message = "供应商不能为空") Long supplierId,
        @PositiveOrZero(message = "成团数量不能小于0") Integer groupQuantity,
        Boolean isDefault,
        @Pattern(regexp = "active|disabled|expired", message = "采购关系状态不合法") String status,
        String remark
) {}
