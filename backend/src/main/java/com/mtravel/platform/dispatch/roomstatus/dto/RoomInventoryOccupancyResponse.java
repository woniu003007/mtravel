package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomLockRecordEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 房态占用明细返回对象。
 *
 * <p>用于点击房态矩阵中的余量或已占数量后，反查哪些团队占用了这一天的库存。</p>
 */
public record RoomInventoryOccupancyResponse(
        Long lockRecordId,
        String sourceType,
        Long sourceId,
        String roomType,
        LocalDate stayDate,
        Integer quantity,
        String teamNo,
        String teamName,
        String status,
        OffsetDateTime createdAt
) {
    /** 将锁房记录拆成某一天的占用明细。 */
    public static RoomInventoryOccupancyResponse fromLock(ControlledRoomLockRecordEntity lock, LocalDate stayDate) {
        return new RoomInventoryOccupancyResponse(
                lock.getId(),
                lock.getSourceType(),
                lock.getSourceId(),
                lock.getRoomType(),
                stayDate,
                lock.getQuantity(),
                lock.getTeamNo(),
                lock.getTeamName(),
                lock.getStatus(),
                lock.getCreatedAt()
        );
    }
}
