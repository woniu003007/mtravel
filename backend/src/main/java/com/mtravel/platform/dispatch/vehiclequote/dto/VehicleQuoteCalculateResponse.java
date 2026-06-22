package com.mtravel.platform.dispatch.vehiclequote.dto;

import java.math.BigDecimal;

/**
 * 用车报价测算结果。
 *
 * @param vehicleType 车辆座位数，字段名沿用 vehicleType 兼容现有接口
 * @param distanceMeters 参与测算的路书距离，单位米
 * @param distanceKilometers 参与测算的路书距离，单位公里
 * @param calculatedAmount 测算参考价
 * @param ruleSnapshot 本次测算命中的规则快照
 */
public record VehicleQuoteCalculateResponse(
        String vehicleType,
        Integer distanceMeters,
        BigDecimal distanceKilometers,
        BigDecimal calculatedAmount,
        VehicleQuoteRuleSnapshotResponse ruleSnapshot
) {}
