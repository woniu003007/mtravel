package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 保存产品级全程用车的完整排序。 */
public record ProductDesignerVehicleArrangementReorderRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotEmpty(message = "用车排序不能为空") List<@NotNull(message = "用车安排ID不能为空") Long> vehicleArrangementIds
) {
}
