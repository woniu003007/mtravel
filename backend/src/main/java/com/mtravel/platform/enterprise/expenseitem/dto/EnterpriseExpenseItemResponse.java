package com.mtravel.platform.enterprise.expenseitem.dto;

import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import java.time.OffsetDateTime;

/**
 * 费用项目接口返回对象。
 *
 * <p>返回资源类型、项目名称、统计标记和排序信息，供费用项目页面和采购价格管理下拉使用。</p>
 */
public record EnterpriseExpenseItemResponse(
        Long id,
        String resourceType,
        String projectName,
        Boolean statisticsEnabled,
        Integer sortOrder,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将数据库实体转换为前端可用的费用项目响应。 */
    public static EnterpriseExpenseItemResponse fromEntity(EnterpriseExpenseItemEntity entity) {
        return new EnterpriseExpenseItemResponse(
                entity.getId(),
                entity.getResourceType(),
                entity.getProjectName(),
                entity.getStatisticsEnabled(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
