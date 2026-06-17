package com.mtravel.platform.purchase.hotel.dto;

import com.mtravel.platform.purchase.hotel.entity.HotelResourceEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 酒店资源返回对象。 */
public record HotelResourceResponse(
        Long id, String hotelName, String city, String area, String address, String starStandard, String roomType,
        Long supplierId, String supplierName, BigDecimal purchasePrice, BigDecimal agreementPrice, String priceUnit,
        LocalDate validFrom, LocalDate validTo, String contactName, String contactPhone, String status, String remark,
        String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static HotelResourceResponse fromEntity(HotelResourceEntity entity, String supplierName) {
        return new HotelResourceResponse(
                entity.getId(), entity.getHotelName(), entity.getCity(), entity.getArea(), entity.getAddress(), entity.getStarStandard(), entity.getRoomType(),
                entity.getSupplierId(), supplierName, entity.getPurchasePrice(), entity.getAgreementPrice(), entity.getPriceUnit(),
                entity.getValidFrom(), entity.getValidTo(), entity.getContactName(), entity.getContactPhone(), entity.getStatus(), entity.getRemark(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
