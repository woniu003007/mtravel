package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import java.math.BigDecimal;
import java.util.List;

/** 产品设计工作台每日资源响应。 */
public record ProductDesignerDayResourceResponse(
        Long id,
        Long productId,
        Integer dayNo,
        Long resourceId,
        String resourceName,
        String resourceType,
        String arrangementRole,
        Boolean hotelBreakfastIncluded,
        String province,
        String city,
        String district,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String procurementMode,
        Integer sortOrder,
        Integer stayMinutes,
        Boolean includeInWord,
        Long supplierId,
        Long supplierRelationId,
        String supplierName,
        String priceMode,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal costAmount,
        Long selectedIntroductionId,
        Integer introductionIndexVersion,
        String introductionTitle,
        String introductionContent,
        String introductionNotice,
        String introductionWarmTip,
        String introductionVisitDuration,
        String remark,
        List<Long> selectedImageIds,
        List<Long> selectedIntroductionIds,
        List<ProductDesignerIntroductionSnapshotResponse> introductionSnapshots,
        List<ProductDesignerSelectedOptionalItemResponse> selectedOptionalItems,
        List<ProductDesignerSelectedMaterialResponse> selectedMaterials
) {
    public static ProductDesignerDayResourceResponse fromEntity(SalesProductDayResourceEntity entity) {
        return fromEntity(entity, List.of());
    }

    public static ProductDesignerDayResourceResponse fromEntity(
            SalesProductDayResourceEntity entity,
            List<Long> selectedImageIds
    ) {
        return fromEntity(entity, selectedImageIds, List.of(), List.of(), List.of(), List.of());
    }

    public static ProductDesignerDayResourceResponse fromEntity(
            SalesProductDayResourceEntity entity,
            List<Long> selectedImageIds,
            List<Long> selectedIntroductionIds,
            List<ProductDesignerIntroductionSnapshotResponse> introductionSnapshots
    ) {
        return fromEntity(entity, selectedImageIds, selectedIntroductionIds, introductionSnapshots, List.of(), List.of());
    }
    public static ProductDesignerDayResourceResponse fromEntity(
            SalesProductDayResourceEntity entity, List<Long> selectedImageIds, List<Long> selectedIntroductionIds,
            List<ProductDesignerIntroductionSnapshotResponse> introductionSnapshots,
            List<ProductDesignerSelectedOptionalItemResponse> selectedOptionalItems
    ) {
        return fromEntity(entity, selectedImageIds, selectedIntroductionIds, introductionSnapshots, selectedOptionalItems, List.of());
    }
    public static ProductDesignerDayResourceResponse fromEntity(
            SalesProductDayResourceEntity entity, List<Long> selectedImageIds, List<Long> selectedIntroductionIds,
            List<ProductDesignerIntroductionSnapshotResponse> introductionSnapshots,
            List<ProductDesignerSelectedOptionalItemResponse> selectedOptionalItems,
            List<ProductDesignerSelectedMaterialResponse> selectedMaterials
    ) {
        return new ProductDesignerDayResourceResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getDayNo(),
                entity.getResourceId(),
                entity.getResourceNameSnapshot(),
                entity.getResourceTypeSnapshot(),
                entity.getArrangementRole(),
                entity.getHotelBreakfastIncluded(),
                entity.getProvinceSnapshot(),
                entity.getCitySnapshot(),
                entity.getDistrictSnapshot(),
                entity.getAddressSnapshot(),
                entity.getLongitudeSnapshot(),
                entity.getLatitudeSnapshot(),
                entity.getProcurementModeSnapshot(),
                entity.getSortOrder(),
                entity.getStayMinutes(),
                entity.getIncludeInWord(),
                entity.getSupplierId(),
                entity.getSupplierRelationIdSnapshot(),
                entity.getSupplierNameSnapshot(),
                entity.getPriceModeSnapshot(),
                entity.getUnitPriceSnapshot(),
                entity.getQuantitySnapshot(),
                entity.getCostAmountSnapshot(),
                entity.getSelectedIntroductionId(),
                entity.getIntroductionIndexVersion(),
                entity.getIntroductionTitleSnapshot(),
                entity.getIntroductionContentSnapshot(),
                entity.getIntroductionNoticeSnapshot(),
                entity.getIntroductionWarmTipSnapshot(),
                entity.getIntroductionVisitDurationSnapshot(),
                entity.getRemark(),
                selectedImageIds == null ? List.of() : selectedImageIds,
                selectedIntroductionIds == null ? List.of() : selectedIntroductionIds,
                introductionSnapshots == null ? List.of() : introductionSnapshots,
                selectedOptionalItems == null ? List.of() : selectedOptionalItems,
                selectedMaterials == null ? List.of() : selectedMaterials
        );
    }
}
