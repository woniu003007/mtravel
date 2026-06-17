package com.mtravel.platform.enterprise.productdictionary.dto;

import com.mtravel.platform.enterprise.productdictionary.entity.EnterpriseProductDictionaryEntity;
import java.time.OffsetDateTime;

/**
 * 产品字典接口返回对象。
 *
 * <p>返回产品模板所需的字典类型、名称、排序和状态信息，供企业资料页面和后续产品页面复用。</p>
 */
public record EnterpriseProductDictionaryResponse(
        Long id,
        String dictType,
        String dictName,
        Integer sortOrder,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将数据库实体转换为前端可用的产品字典响应。 */
    public static EnterpriseProductDictionaryResponse fromEntity(EnterpriseProductDictionaryEntity entity) {
        return new EnterpriseProductDictionaryResponse(
                entity.getId(),
                entity.getDictType(),
                entity.getDictName(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
