package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomLockRecordEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 自控房源锁房流水返回对象。
 */
public record ControlledRoomLockRecordResponse(
        Long id,
        Long resourceId,
        Long roomId,
        String hotelName,
        String roomNo,
        String roomType,
        String starStandard,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String teamNo,
        String teamName,
        String requiredStandard,
        String status,
        OffsetDateTime releasedAt,
        String releasedBy,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将锁房流水和房源/房间展示信息组装成接口返回结构。 */
    public static ControlledRoomLockRecordResponse fromEntity(
            ControlledRoomLockRecordEntity entity,
            String hotelName,
            String roomNo,
            String roomType,
            String starStandard
    ) {
        return new ControlledRoomLockRecordResponse(
                entity.getId(),
                entity.getResourceId(),
                entity.getRoomId(),
                hotelName,
                roomNo,
                roomType,
                starStandard,
                entity.getCheckInDate(),
                entity.getCheckOutDate(),
                entity.getTeamNo(),
                entity.getTeamName(),
                entity.getRequiredStandard(),
                entity.getStatus(),
                entity.getReleasedAt(),
                entity.getReleasedBy(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
