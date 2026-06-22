package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductVehicleQuoteSnapshotEntity;
import java.math.BigDecimal;

/**
 * 销售产品用车报价测算快照响应对象。
 *
 * <p>用于产品团队安排页回显路书公里、测算规则快照和参考金额。</p>
 */
public record SalesProductVehicleQuoteSnapshotResponse(
        Long id,
        String scheduleStartDay,
        String scheduleEndDay,
        Integer startDayNo,
        Integer endDayNo,
        Integer syncedDistanceMeters,
        Integer syncedDurationSeconds,
        String routeSummary,
        Long quoteRuleId,
        String ruleVehicleType,
        String ruleProvince,
        String ruleCity,
        String ruleDistrict,
        BigDecimal ruleBasePrice,
        BigDecimal ruleBaseKilometers,
        BigDecimal ruleExtraKilometerPrice,
        BigDecimal ruleMinimumPrice,
        BigDecimal ruleFloatRate,
        BigDecimal calculatedAmount,
        BigDecimal confirmedAmount,
        String remark
) {
    /** 将快照实体转换为接口响应。 */
    public static SalesProductVehicleQuoteSnapshotResponse fromEntity(SalesProductVehicleQuoteSnapshotEntity entity) {
        return new SalesProductVehicleQuoteSnapshotResponse(
                entity.getId(),
                entity.getScheduleStartDay(),
                entity.getScheduleEndDay(),
                entity.getStartDayNo(),
                entity.getEndDayNo(),
                entity.getSyncedDistanceMeters(),
                entity.getSyncedDurationSeconds(),
                entity.getRouteSummary(),
                entity.getQuoteRuleId(),
                entity.getRuleVehicleType(),
                entity.getRuleProvince(),
                entity.getRuleCity(),
                entity.getRuleDistrict(),
                entity.getRuleBasePrice(),
                entity.getRuleBaseKilometers(),
                entity.getRuleExtraKilometerPrice(),
                entity.getRuleMinimumPrice(),
                entity.getRuleFloatRate(),
                entity.getCalculatedAmount(),
                entity.getConfirmedAmount(),
                entity.getRemark()
        );
    }
}
