package com.mtravel.platform.dispatch.vehiclequote.dto;

import com.mtravel.platform.dispatch.vehiclequote.entity.VehicleQuoteRuleEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 座位数报价规则响应对象。
 *
 * <p>用于报价规则列表、编辑回显和测算规则选择。</p>
 */
public record VehicleQuoteRuleResponse(
        Long id,
        String vehicleType,
        String province,
        String city,
        String district,
        BigDecimal basePrice,
        BigDecimal baseKilometers,
        BigDecimal extraKilometerPrice,
        BigDecimal minimumPrice,
        BigDecimal floatRate,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将实体转换为接口响应。 */
    public static VehicleQuoteRuleResponse fromEntity(VehicleQuoteRuleEntity entity) {
        return new VehicleQuoteRuleResponse(
                entity.getId(),
                entity.getVehicleType(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getBasePrice(),
                entity.getBaseKilometers(),
                entity.getExtraKilometerPrice(),
                entity.getMinimumPrice(),
                entity.getFloatRate(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
