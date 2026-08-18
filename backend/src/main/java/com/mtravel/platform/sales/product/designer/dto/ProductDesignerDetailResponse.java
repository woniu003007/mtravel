package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;
import java.util.List;

/** 产品设计工作台详情响应。 */
public record ProductDesignerDetailResponse(
        Long productId,
        String productName,
        String province,
        String city,
        Integer travelDays,
        String status,
        BigDecimal totalCostAmount,
        List<ProductDesignerDayPlanResponse> days,
        ProductDesignerAdultQuoteResponse adultQuote
) {}
