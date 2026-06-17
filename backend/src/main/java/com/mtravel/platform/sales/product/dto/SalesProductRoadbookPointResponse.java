package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductRoadbookPointEntity;

/**
 * 销售产品每日路书地点返回对象。
 *
 * <p>用于产品详情页回显地图路书点位和路线计算结果。</p>
 */
public record SalesProductRoadbookPointResponse(
        Long id,
        Integer pointOrder,
        String placeName,
        String address,
        String longitude,
        String latitude,
        String pointType,
        Integer stayMinutes,
        Integer distanceToNextMeters,
        Integer durationToNextSeconds,
        String remark
) {
    /** 将路书地点实体转换为接口响应。 */
    public static SalesProductRoadbookPointResponse fromEntity(SalesProductRoadbookPointEntity entity) {
        return new SalesProductRoadbookPointResponse(
                entity.getId(),
                entity.getPointOrder(),
                entity.getPlaceName(),
                entity.getAddress(),
                entity.getLongitude(),
                entity.getLatitude(),
                entity.getPointType(),
                entity.getStayMinutes(),
                entity.getDistanceToNextMeters(),
                entity.getDurationToNextSeconds(),
                entity.getRemark()
        );
    }
}
