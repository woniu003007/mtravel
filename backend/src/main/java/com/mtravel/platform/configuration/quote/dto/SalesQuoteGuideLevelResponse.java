package com.mtravel.platform.configuration.quote.dto;

import com.mtravel.platform.configuration.quote.entity.SalesQuoteGuideLevelEntity;
import java.time.OffsetDateTime;

/**
 * 导游等级返回对象。
 */
public record SalesQuoteGuideLevelResponse(
        Long id,
        String levelName,
        Integer sortOrder,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将实体转换为接口返回对象。 */
    public static SalesQuoteGuideLevelResponse fromEntity(SalesQuoteGuideLevelEntity entity) {
        return new SalesQuoteGuideLevelResponse(
                entity.getId(),
                entity.getLevelName(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
