package com.mtravel.platform.configuration.quote.dto;

import com.mtravel.platform.configuration.quote.entity.SalesQuoteResourceRuleEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 普通资源销售报价规则返回对象。
 */
public record SalesQuoteResourceRuleResponse(
        Long id,
        String resourceType,
        Long customerCategoryId,
        String customerCategoryName,
        String quoteMode,
        BigDecimal suggestedMarkupRate,
        BigDecimal minimumMarkupRate,
        BigDecimal suggestedFixedMarkup,
        BigDecimal minimumFixedMarkup,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将实体转换为接口返回对象。 */
    public static SalesQuoteResourceRuleResponse fromEntity(SalesQuoteResourceRuleEntity entity) {
        return new SalesQuoteResourceRuleResponse(
                entity.getId(),
                entity.getResourceType(),
                entity.getCustomerCategoryId(),
                entity.getCustomerCategoryName(),
                entity.getQuoteMode(),
                entity.getSuggestedMarkupRate(),
                entity.getMinimumMarkupRate(),
                entity.getSuggestedFixedMarkup(),
                entity.getMinimumFixedMarkup(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
