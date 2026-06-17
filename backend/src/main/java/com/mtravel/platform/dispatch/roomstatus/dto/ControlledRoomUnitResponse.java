package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomUnitEntity;
import java.time.OffsetDateTime;

/**
 * 自控房间明细返回对象。
 */
public record ControlledRoomUnitResponse(
        Long id,
        Long resourceId,
        String hotelName,
        String starStandard,
        String buildingName,
        String floorNo,
        String roomNo,
        String roomType,
        String bedType,
        Integer capacity,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将房间实体和所属房源关键信息组装成接口返回结构。 */
    public static ControlledRoomUnitResponse fromEntity(
            ControlledRoomUnitEntity entity,
            String hotelName,
            String starStandard
    ) {
        return new ControlledRoomUnitResponse(
                entity.getId(),
                entity.getResourceId(),
                hotelName,
                starStandard,
                entity.getBuildingName(),
                entity.getFloorNo(),
                entity.getRoomNo(),
                entity.getRoomType(),
                entity.getBedType(),
                entity.getCapacity(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
