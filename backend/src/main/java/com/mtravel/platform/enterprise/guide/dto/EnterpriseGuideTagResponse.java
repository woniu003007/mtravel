package com.mtravel.platform.enterprise.guide.dto;

import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagEntity;
import java.time.OffsetDateTime;

/**
 * 导游标签返回对象。
 *
 * <p>导游档案列表、编辑表单和标签管理页共用该对象。</p>
 */
public record EnterpriseGuideTagResponse(
        Long id,
        String tagName,
        Integer sortOrder,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将导游标签实体转换为接口返回对象。 */
    public static EnterpriseGuideTagResponse fromEntity(EnterpriseGuideTagEntity entity) {
        return new EnterpriseGuideTagResponse(
                entity.getId(),
                entity.getTagName(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
