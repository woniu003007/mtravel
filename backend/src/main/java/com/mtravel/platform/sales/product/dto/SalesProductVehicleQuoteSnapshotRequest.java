package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 销售产品用车报价测算快照保存请求。
 *
 * <p>该对象保存产品团队安排用车时的测算结果。它是当时参考价快照，不随报价规则后续修改自动变化。</p>
 */
public record SalesProductVehicleQuoteSnapshotRequest(
        @Size(max = 40) String scheduleStartDay,
        @Size(max = 40) String scheduleEndDay,
        @Min(value = 1, message = "开始天数必须从1开始") Integer startDayNo,
        @Min(value = 1, message = "结束天数必须从1开始") Integer endDayNo,
        @Min(value = 0, message = "路书距离不能小于0") Integer syncedDistanceMeters,
        @Min(value = 0, message = "预计车程不能小于0") Integer syncedDurationSeconds,
        @Size(max = 1000) String routeSummary,
        Long quoteRuleId,
        @Size(max = 40) String ruleVehicleType,
        @Size(max = 80) String ruleProvince,
        @Size(max = 80) String ruleCity,
        @Size(max = 80) String ruleDistrict,
        @DecimalMin(value = "0", message = "规则基础价不能小于0") BigDecimal ruleBasePrice,
        @DecimalMin(value = "0", message = "规则基础公里不能小于0") BigDecimal ruleBaseKilometers,
        @DecimalMin(value = "0", message = "规则超公里单价不能小于0") BigDecimal ruleExtraKilometerPrice,
        @DecimalMin(value = "0", message = "规则最低价不能小于0") BigDecimal ruleMinimumPrice,
        @DecimalMin(value = "0", inclusive = false, message = "规则浮动系数必须大于0") BigDecimal ruleFloatRate,
        @DecimalMin(value = "0", message = "测算金额不能小于0") BigDecimal calculatedAmount,
        @DecimalMin(value = "0", message = "确认金额不能小于0") BigDecimal confirmedAmount,
        String remark
) {}
