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
        String supplierName,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal costAmount,
        Long selectedIntroductionId,
        Integer introductionIndexVersion,
        String introductionTitle,
        String introductionContent,
        String introductionNotice,
        String remark,
        List<Long> selectedImageIds
) {
    public static ProductDesignerDayResourceResponse fromEntity(SalesProductDayResourceEntity entity) {
        return fromEntity(entity, List.of());
    }

    public static ProductDesignerDayResourceResponse fromEntity(
            SalesProductDayResourceEntity entity,
            List<Long> selectedImageIds
    ) {
        return new ProductDesignerDayResourceResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getDayNo(),
                entity.getResourceId(),
                entity.getResourceNameSnapshot(),
                entity.getResourceTypeSnapshot(),
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
                entity.getSupplierNameSnapshot(),
                entity.getUnitPriceSnapshot(),
                entity.getQuantitySnapshot(),
                entity.getCostAmountSnapshot(),
                entity.getSelectedIntroductionId(),
                entity.getIntroductionIndexVersion(),
                entity.getIntroductionTitleSnapshot(),
                entity.getIntroductionContentSnapshot(),
                entity.getIntroductionNoticeSnapshot(),
                entity.getRemark(),
                selectedImageIds == null ? List.of() : selectedImageIds
        );
    }
}
