package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.NotNull;

/** 单独保存每日资源介绍版本选择的请求。 */
public record ProductDesignerIntroductionSaveRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "每日资源ID不能为空") Long dayResourceId,
        Long selectedIntroductionId
) {}
