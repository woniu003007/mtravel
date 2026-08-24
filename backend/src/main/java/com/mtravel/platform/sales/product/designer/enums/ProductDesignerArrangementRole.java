package com.mtravel.platform.sales.product.designer.enums;

import com.mtravel.platform.common.BizException;
import java.util.Arrays;

/** 产品设计工作台日资源的明确编排区块。 */
public enum ProductDesignerArrangementRole {
    ACCOMMODATION("accommodation"),
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    ITINERARY("itinerary"),
    GROUND_SERVICE("ground_service"),
    LEGACY_UNASSIGNED("unassigned");

    private final String value;

    ProductDesignerArrangementRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean isMeal() {
        return this == BREAKFAST || this == LUNCH || this == DINNER;
    }

    public boolean isIndependentlySortable() {
        return this == ACCOMMODATION || this == ITINERARY || this == GROUND_SERVICE;
    }

    public static ProductDesignerArrangementRole fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException("资源安排位置不合法"));
    }
}
