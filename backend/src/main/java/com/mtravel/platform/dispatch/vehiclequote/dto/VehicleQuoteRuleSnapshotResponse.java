package com.mtravel.platform.dispatch.vehiclequote.dto;

import com.mtravel.platform.dispatch.vehiclequote.entity.VehicleQuoteRuleEntity;
import java.math.BigDecimal;

/**
 * 座位数报价规则快照。
 *
 * <p>产品团队安排保存测算结果时需要保留规则当时的价格参数，避免以后规则调整导致历史产品参考价变化。</p>
 */
public record VehicleQuoteRuleSnapshotResponse(
        Long ruleId,
        String vehicleType,
        String province,
        String city,
        String district,
        BigDecimal basePrice,
        BigDecimal baseKilometers,
        BigDecimal extraKilometerPrice,
        BigDecimal minimumPrice,
        BigDecimal floatRate
) {
    /** 从当前报价规则构造可保存的快照。 */
    public static VehicleQuoteRuleSnapshotResponse fromEntity(VehicleQuoteRuleEntity entity) {
        return new VehicleQuoteRuleSnapshotResponse(
                entity.getId(),
                entity.getVehicleType(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getBasePrice(),
                entity.getBaseKilometers(),
                entity.getExtraKilometerPrice(),
                entity.getMinimumPrice(),
                entity.getFloatRate()
        );
    }
}
