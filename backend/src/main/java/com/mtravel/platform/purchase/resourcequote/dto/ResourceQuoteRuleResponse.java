package com.mtravel.platform.purchase.resourcequote.dto;

import com.mtravel.platform.purchase.resourcequote.entity.ResourceQuoteRuleEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 普通资源报价规则响应对象。
 *
 * <p>比例字段保持百分数语义，不在接口层换算为小数。</p>
 */
public record ResourceQuoteRuleResponse(
        Long id,
        String resourceType,
        Long customerLevelId,
        String customerLevelName,
        BigDecimal suggestedRate,
        BigDecimal minimumRate,
        BigDecimal suggestedFixedAddon,
        BigDecimal minimumFixedAddon,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将数据库实体及客户等级展示名称转换为接口响应。 */
    public static ResourceQuoteRuleResponse fromEntity(ResourceQuoteRuleEntity entity, String customerLevelName) {
        return new ResourceQuoteRuleResponse(
                entity.getId(),
                entity.getResourceType(),
                entity.getCustomerLevelId(),
                customerLevelName,
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
