package com.mtravel.platform.purchase.relation.dto;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

/**
 * 采购关系返回对象。
 *
 * <p>列表字段与旧系统保持一致，所在地来自资源主档，负责人和电话来自供应商档案。
 * 成团数量字段继续保留在接口中兼容历史数据，但前端列表不再展示。</p>
 */
public record PurchaseRelationResponse(
        Long id, String resourceType, Long resourceId, String resourceName, Long supplierId, String supplierName,
        String location, String contactName, String contactPhone,
        Integer groupQuantity, String status, String remark, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    /**
     * 将采购关系、资源主档和供应商档案组装为前端列表行。
     *
     * <p>关联主档缺失时保留采购关系自身信息，其余展示字段返回空值，避免历史关系无法查看。</p>
     */
    public static PurchaseRelationResponse fromEntities(
            PurchaseRelationEntity entity,
            PurchaseResourceEntity resource,
            SupplierEntity supplier
    ) {
        return new PurchaseRelationResponse(
                entity.getId(), entity.getResourceType(), entity.getResourceId(), entity.getResourceName(), entity.getSupplierId(),
                supplier == null ? null : supplier.getSupplierName(),
                location(resource),
                supplier == null ? null : supplier.getContactName(),
                supplier == null ? null : supplier.getContactPhone(),
                entity.getGroupQuantity(), entity.getStatus(), entity.getRemark(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    /** 将非空的省、市、区县按页面展示格式拼接。 */
    private static String location(PurchaseResourceEntity resource) {
        if (resource == null) {
            return null;
        }
        String value = Stream.of(resource.getProvince(), resource.getCity(), resource.getDistrict())
                .filter(item -> item != null && !item.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
        return value;
    }
}
