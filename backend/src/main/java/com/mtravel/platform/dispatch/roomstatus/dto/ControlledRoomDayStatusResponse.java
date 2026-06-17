package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomDayStatusEntity;
import java.time.LocalDate;

/**
 * 自控房间每日房态返回对象。
 */
public record ControlledRoomDayStatusResponse(
        Long id,
        Long resourceId,
        Long roomId,
        String hotelName,
        String roomNo,
        String roomType,
        String starStandard,
        LocalDate stayDate,
        String status,
        Long lockRecordId,
        String teamNo,
        String teamName,
        String remark
) {
    /** 将每日房态和房源/房间展示信息组装成接口返回结构。 */
    public static ControlledRoomDayStatusResponse fromEntity(
            ControlledRoomDayStatusEntity entity,
            String hotelName,
            String roomNo,
            String roomType,
            String starStandard
    ) {
        return new ControlledRoomDayStatusResponse(
                entity.getId(),
                entity.getResourceId(),
                entity.getRoomId(),
                hotelName,
                roomNo,
                roomType,
                starStandard,
                entity.getStayDate(),
                entity.getStatus(),
                entity.getLockRecordId(),
                entity.getTeamNo(),
                entity.getTeamName(),
                entity.getRemark()
        );
    }
}
