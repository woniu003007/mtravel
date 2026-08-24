package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.NotNull;

/** 仅替换一条已编排行资源的供应商关系和成本快照。 */
public record ProductDesignerDayResourceSupplierSaveRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "每日资源ID不能为空") Long dayResourceId,
        @NotNull(message = "供应商采购关系ID不能为空") Long supplierRelationId
) {}
