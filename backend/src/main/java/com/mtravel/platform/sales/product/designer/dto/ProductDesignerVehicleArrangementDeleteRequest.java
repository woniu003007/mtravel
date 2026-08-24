package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.NotNull;

/** 删除产品设计工作台的一条产品级用车安排。 */
public record ProductDesignerVehicleArrangementDeleteRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "用车安排ID不能为空") Long vehicleArrangementId
) {
}
