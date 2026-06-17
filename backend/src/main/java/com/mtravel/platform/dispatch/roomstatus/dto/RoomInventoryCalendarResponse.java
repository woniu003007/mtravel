package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.RoomInventoryEntity;
import java.time.LocalDate;

/**
 * 房态库存日历行。
 *
 * <p>用于前端按酒店、来源、房型和日期展示总量、已锁、已占和余量。</p>
 */
public record RoomInventoryCalendarResponse(
        Long id,
        String sourceType,
        Long sourceId,
        Long roomTypeId,
        String hotelName,
        String supplierName,
        String roomType,
        LocalDate stayDate,
        Integer totalQuantity,
        Integer lockedQuantity,
        Integer occupiedQuantity,
        Integer remainingQuantity,
        String status
) {
    /** 从库存实体构造日历返回对象。 */
    public static RoomInventoryCalendarResponse fromEntity(RoomInventoryEntity entity) {
        return new RoomInventoryCalendarResponse(
                entity.getId(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getRoomTypeId(),
                entity.getHotelName(),
                entity.getSupplierName(),
                entity.getRoomType(),
                entity.getStayDate(),
                entity.getTotalQuantity(),
                entity.getLockedQuantity(),
                entity.getOccupiedQuantity(),
                entity.getRemainingQuantity(),
                entity.getStatus()
        );
    }
}
