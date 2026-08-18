package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;
import java.util.List;

/** 产品工作台某一天的资源编排响应。 */
public record ProductDesignerDayPlanResponse(
        Integer dayNo,
        BigDecimal dayCostAmount,
        List<ProductDesignerDayResourceResponse> resources
) {}
