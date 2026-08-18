package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.NotNull;

/** 产品设计工作台单条每日资源删除请求。 */
public record ProductDesignerDayResourceDeleteRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "每日资源ID不能为空") Long id
) {}
