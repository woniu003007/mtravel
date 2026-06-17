package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomTypeEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 自营房型返回对象。
 */
public record ControlledRoomTypeResponse(
        Long id,
        Long resourceId,
        String hotelName,
        String roomType,
        String bedType,
        Integer capacity,
        BigDecimal purchasePrice,
        BigDecimal agreementPrice,
        String priceUnit,
        String status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将房型实体和酒店名称快照组装为接口返回结构。 */
    public static ControlledRoomTypeResponse fromEntity(ControlledRoomTypeEntity entity, String hotelName) {
        return new ControlledRoomTypeResponse(
                entity.getId(),
                entity.getResourceId(),
                hotelName,
                entity.getRoomType(),
                entity.getBedType(),
                entity.getCapacity(),
                entity.getPurchasePrice(),
                entity.getAgreementPrice(),
                entity.getPriceUnit(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
