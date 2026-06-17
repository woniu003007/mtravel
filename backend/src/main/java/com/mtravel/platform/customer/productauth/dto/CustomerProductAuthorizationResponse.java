package com.mtravel.platform.customer.productauth.dto;

import com.mtravel.platform.customer.productauth.entity.CustomerProductAuthorizationEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 客户产品授权返回对象。 */
public record CustomerProductAuthorizationResponse(
        Long id, Long customerId, String customerName, String productCode, String productName,
        LocalDate authorizedStartDate, LocalDate authorizedEndDate, String authorizationStatus,
        String saleScope, String remark, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CustomerProductAuthorizationResponse fromEntity(CustomerProductAuthorizationEntity entity, String customerName) {
        return new CustomerProductAuthorizationResponse(
                entity.getId(), entity.getCustomerId(), customerName, entity.getProductCode(), entity.getProductName(),
                entity.getAuthorizedStartDate(), entity.getAuthorizedEndDate(), entity.getAuthorizationStatus(),
                entity.getSaleScope(), entity.getRemark(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
