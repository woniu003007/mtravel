package com.mtravel.platform.dispatch.roomstatus.dto;

import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomResourceEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 自控房源批次返回对象。
 */
public record ControlledRoomResourceResponse(
        Long id,
        String hotelName,
        String province,
        String city,
        String district,
        String area,
        String address,
        String starStandard,
        String roomType,
        String sourceName,
        BigDecimal purchasePrice,
        BigDecimal agreementPrice,
        String priceUnit,
        LocalDate validFrom,
        LocalDate validTo,
        String contactName,
        String contactPhone,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将实体组装成接口返回结构。 */
    public static ControlledRoomResourceResponse fromEntity(ControlledRoomResourceEntity entity) {
        return new ControlledRoomResourceResponse(
                entity.getId(),
                entity.getHotelName(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getArea(),
                entity.getAddress(),
                entity.getStarStandard(),
                entity.getRoomType(),
                entity.getSourceName(),
                entity.getPurchasePrice(),
                entity.getAgreementPrice(),
                entity.getPriceUnit(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
