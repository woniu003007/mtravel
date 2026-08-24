package com.mtravel.platform.sales.product.designer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 产品设计工作台产品级全程用车保存请求。 */
@Schema(name = "ProductDesignerVehicleArrangementSaveRequest", description = "产品设计工作台全程用车保存参数")
public record ProductDesignerVehicleArrangementSaveRequest(
        @Schema(description = "已有用车安排 ID，空表示新增") Long id,
        @NotNull(message = "产品ID不能为空") Long productId,
        @Schema(description = "用车资源 ID；待询价手工用车可以为空") Long resourceId,
        @Schema(description = "选中的供应商采购关系 ID") Long supplierRelationId,
        @Min(value = 1, message = "起始天数必须从1开始") Integer startDayNo,
        @Min(value = 1, message = "结束天数必须从1开始") Integer endDayNo,
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,
        @Size(max = 120, message = "车型不能超过120个字符") String vehicleType,
        @Min(value = 1, message = "排序必须从1开始") Integer sortOrder,
        String remark
) {
}
