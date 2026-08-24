package com.mtravel.platform.sales.product.designer.dto;

/** 次日早餐由前一晚承接时，对应酒店的不可变快照。 */
public record ProductDesignerBreakfastHotelResponse(Long dayResourceId, Long resourceId, String resourceName) {}
