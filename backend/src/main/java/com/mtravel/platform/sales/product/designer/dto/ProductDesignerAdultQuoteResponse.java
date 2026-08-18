package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 产品成人报价草稿响应。 */
public record ProductDesignerAdultQuoteResponse(
        Long id,
        Long productId,
        Integer plannedAdultCount,
        BigDecimal adultCostAmount,
        BigDecimal markupAmount,
        BigDecimal adultSaleAmount,
        LocalDate validUntil,
        String quoteRemark,
        String status
) {
    public static ProductDesignerAdultQuoteResponse fromEntity(SalesProductAdultQuoteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProductDesignerAdultQuoteResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getPlannedAdultCount(),
                entity.getAdultCostAmount(),
                entity.getMarkupAmount(),
                entity.getAdultSaleAmount(),
                entity.getValidUntil(),
                entity.getQuoteRemark(),
                entity.getStatus()
        );
    }
}
