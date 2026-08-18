package com.mtravel.platform.configuration.quote.dto;

import com.mtravel.platform.configuration.quote.entity.SalesQuoteGuideRuleEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 导游销售报价规则返回对象。
 */
public record SalesQuoteGuideRuleResponse(
        Long id,
        Long guideLevelId,
        String guideLevelName,
        String language,
        BigDecimal baseDailyFee,
        BigDecimal foreignLanguageDailyMarkup,
        BigDecimal overtimeHourlyFee,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将实体转换为接口返回对象。 */
    public static SalesQuoteGuideRuleResponse fromEntity(SalesQuoteGuideRuleEntity entity) {
        return new SalesQuoteGuideRuleResponse(
                entity.getId(),
                entity.getGuideLevelId(),
                entity.getGuideLevelName(),
                entity.getLanguage(),
                entity.getBaseDailyFee(),
                entity.getForeignLanguageDailyMarkup(),
                entity.getOvertimeHourlyFee(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
