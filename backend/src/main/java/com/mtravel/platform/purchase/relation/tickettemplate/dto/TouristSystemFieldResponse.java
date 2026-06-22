package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import com.mtravel.platform.purchase.relation.tickettemplate.enums.TouristSystemField;
import java.util.Arrays;
import java.util.List;

/**
 * 系统游客字段选项返回对象。
 */
public record TouristSystemFieldResponse(String value, String label) {
    /** 返回前端可选字段列表。 */
    public static List<TouristSystemFieldResponse> all() {
        return Arrays.stream(TouristSystemField.values())
                .map(item -> new TouristSystemFieldResponse(item.value(), item.label()))
                .toList();
    }
}
