package com.mtravel.platform.sales.product.dto;

import java.util.List;

/**
 * 高德驾车路线计算结果。
 *
 * @param totalDistanceMeters 总距离，单位米
 * @param totalDurationSeconds 总预计车程，单位秒
 * @param segments 分段结果，按相邻点顺序返回
 */
public record AmapRouteCalculateResponse(
        Integer totalDistanceMeters,
        Integer totalDurationSeconds,
        List<AmapRouteSegmentResponse> segments
) {}
