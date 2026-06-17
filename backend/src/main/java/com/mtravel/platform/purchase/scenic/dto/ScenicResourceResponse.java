package com.mtravel.platform.purchase.scenic.dto;

import com.mtravel.platform.purchase.scenic.entity.ScenicResourceEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 景区资源返回对象。 */
public record ScenicResourceResponse(
        Long id, String scenicName, String city, String area, String address, String ticketType,
        Long supplierId, String supplierName, BigDecimal purchasePrice, BigDecimal agreementPrice, String priceUnit,
        LocalDate validFrom, LocalDate validTo, String freeTicketRule, String halfTicketRule,
        String contactName, String contactPhone, String status, String remark,
        String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static ScenicResourceResponse fromEntity(ScenicResourceEntity entity, String supplierName) {
        return new ScenicResourceResponse(
                entity.getId(), entity.getScenicName(), entity.getCity(), entity.getArea(), entity.getAddress(), entity.getTicketType(),
                entity.getSupplierId(), supplierName, entity.getPurchasePrice(), entity.getAgreementPrice(), entity.getPriceUnit(),
                entity.getValidFrom(), entity.getValidTo(), entity.getFreeTicketRule(), entity.getHalfTicketRule(),
                entity.getContactName(), entity.getContactPhone(), entity.getStatus(), entity.getRemark(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
