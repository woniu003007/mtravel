package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import java.time.OffsetDateTime;

/**
 * 采购资源列表和详情返回对象。
 *
 * @param boundSupplierCount 当前资源已绑定的供应商数量
 */
public record PurchaseResourceResponse(
        Long id,
        String resourceType,
        String resourceName,
        String province,
        String city,
        String district,
        String phone,
        String fax,
        String address,
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
                entity.getResourceName(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getPhone(),
                entity.getFax(),
                entity.getAddress(),
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
