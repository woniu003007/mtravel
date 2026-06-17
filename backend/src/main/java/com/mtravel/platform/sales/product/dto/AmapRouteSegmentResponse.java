package com.mtravel.platform.sales.product.dto;

/**
 * 高德路线分段结果。
 *
 * @param distanceMeters 分段距离，单位米
 * @param durationSeconds 分段预计车程，单位秒
 */
public record AmapRouteSegmentResponse(
        Integer distanceMeters,
        Integer durationSeconds
) {}
