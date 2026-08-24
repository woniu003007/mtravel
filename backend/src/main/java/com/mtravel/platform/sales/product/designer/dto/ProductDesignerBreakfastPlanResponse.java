package com.mtravel.platform.sales.product.designer.dto;

import java.util.List;

/** 后端统一解析的早餐来源，避免页面和 Word 分别推导前夜酒店逻辑。 */
public record ProductDesignerBreakfastPlanResponse(
        String source,
        List<ProductDesignerBreakfastHotelResponse> hotelSources,
        ProductDesignerDayResourceResponse restaurant
) {}
