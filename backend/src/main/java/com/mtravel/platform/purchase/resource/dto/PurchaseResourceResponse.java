package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * 采购资源列表和详情返回对象。
 *
 * @param boundSupplierCount 当前资源已绑定的供应商数量
 */
public record PurchaseResourceResponse(
        Long id,
        String resourceType,
        String procurementMode,
        String resourceName,
        String province,
        String city,
        String district,
        String phone,
        String contactName,
        String fax,
        String address,
        String scenicLevel,
        String starLevel,
        String categoryTags,
        BigDecimal longitude,
        BigDecimal latitude,
        String businessStatus,
        LocalTime openingTime,
        LocalTime closingTime,
        String siteVisitStatus,
        LocalDate lastSiteVisitDate,
        String siteVisitNote,
        Integer capacity,
        Integer tableCount,
        String mealStandard,
        String vehicleType,
        Integer seatCount,
        String billingMode,
        String serviceArea,
        Integer referenceDays,
        String includedItems,
        String excludedItems,
        String resourceUnit,
        String warmTip,
        String introduction,
        String status,
        Long boundSupplierCount,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将实体和绑定供应商数量组装成接口返回结构。 */
    public static PurchaseResourceResponse fromEntity(PurchaseResourceEntity entity, Long boundSupplierCount) {
        return new PurchaseResourceResponse(
                entity.getId(),
                entity.getResourceType(),
                entity.getProcurementMode(),
                entity.getResourceName(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getPhone(),
                entity.getContactName(),
                entity.getFax(),
                entity.getAddress(),
                entity.getScenicLevel(),
                entity.getStarLevel(),
                entity.getCategoryTags(),
                entity.getLongitude(),
                entity.getLatitude(),
                entity.getBusinessStatus(),
                entity.getOpeningTime(),
                entity.getClosingTime(),
                entity.getSiteVisitStatus(),
                entity.getLastSiteVisitDate(),
                entity.getSiteVisitNote(),
                entity.getCapacity(),
                entity.getTableCount(),
                entity.getMealStandard(),
                entity.getVehicleType(),
                entity.getSeatCount(),
                entity.getBillingMode(),
                entity.getServiceArea(),
                entity.getReferenceDays(),
                entity.getIncludedItems(),
                entity.getExcludedItems(),
                entity.getResourceUnit(),
                entity.getWarmTip(),
                entity.getIntroduction(),
                entity.getStatus(),
                boundSupplierCount == null ? 0L : boundSupplierCount,
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
