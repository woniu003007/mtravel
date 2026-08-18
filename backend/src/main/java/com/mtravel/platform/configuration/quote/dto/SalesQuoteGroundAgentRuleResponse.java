package com.mtravel.platform.configuration.quote.dto;

import com.mtravel.platform.configuration.quote.entity.SalesQuoteGroundAgentRuleEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 地接整团打包报价规则返回对象。
 */
public record SalesQuoteGroundAgentRuleResponse(
        Long id,
        Integer minPeople,
        Integer maxPeople,
        BigDecimal groupPackagePrice,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将实体转换为接口返回对象。 */
    public static SalesQuoteGroundAgentRuleResponse fromEntity(SalesQuoteGroundAgentRuleEntity entity) {
        return new SalesQuoteGroundAgentRuleResponse(
                entity.getId(),
                entity.getMinPeople(),
                entity.getMaxPeople(),
                entity.getGroupPackagePrice(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
