package com.mtravel.platform.purchase.supplier.dto;

import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import java.time.OffsetDateTime;

/** 供应商返回对象。 */
public record SupplierResponse(
        Long id, String supplierCode, String supplierName, String supplierCategory,
        Long buyerId, String buyerName,
        String province, String city, String district, String settlementMethod,
        String contactName, String contactPhone, String faxNumber, String officeAddress, String agreementName, Integer rating,
        String status, String remark, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static SupplierResponse fromEntity(SupplierEntity entity) {
        return fromEntity(entity, null);
    }

    public static SupplierResponse fromEntity(SupplierEntity entity, String buyerName) {
        return new SupplierResponse(
                entity.getId(), entity.getSupplierCode(), entity.getSupplierName(), entity.getSupplierCategory(),
                entity.getBuyerId(), buyerName,
                entity.getProvince(), entity.getCity(), entity.getDistrict(), entity.getSettlementMethod(),
                entity.getContactName(), entity.getContactPhone(), entity.getFaxNumber(), entity.getOfficeAddress(), entity.getAgreementName(), entity.getRating(),
                entity.getStatus(), entity.getRemark(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
