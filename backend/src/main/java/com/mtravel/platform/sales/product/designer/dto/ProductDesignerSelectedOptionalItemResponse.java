package com.mtravel.platform.sales.product.designer.dto;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceOptionalItemEntity;
import java.math.BigDecimal;
/** 产品自费项目快照响应；成本只对内部工作台返回，Word不使用。 */
public record ProductDesignerSelectedOptionalItemResponse(Long id, Long resourceOptionalItemId, String projectName, String optionalItemType, Long supplierOptionalItemId, BigDecimal referenceCostPrice, BigDecimal suggestedSalePrice, BigDecimal salePrice, Long introductionId, String introductionContent, Integer sortOrder) {
 public static ProductDesignerSelectedOptionalItemResponse fromEntity(SalesProductDayResourceOptionalItemEntity e){return new ProductDesignerSelectedOptionalItemResponse(e.getId(),e.getResourceOptionalItemId(),e.getProjectNameSnapshot(),e.getItemTypeSnapshot(),e.getSupplierOptionalItemId(),e.getSupplierCostPriceSnapshot(),e.getSuggestedSalePriceSnapshot(),e.getFinalSalePrice(),e.getSelectedIntroductionId(),e.getIntroductionContentSnapshot(),e.getSortOrder());}
}
