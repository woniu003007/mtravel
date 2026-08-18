package com.mtravel.platform.sales.product.designer.dto;

import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import java.time.OffsetDateTime;

/**
 * 产品设计草稿列表和详情响应。
 *
 * @param id 草稿ID
 * @param productName 产品名称
 * @param businessType 业务类型
 * @param domesticInternational 国内国际标记
 * @param province 接团省份
 * @param city 接团城市
 * @param district 接团区县
 * @param receptionStandard 接待标准
 * @param productTheme 产品主题
 * @param travelDays 旅游天数
 * @param designStatus 设计状态
 * @param remark 设计备注
 * @param createdBy 创建人
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ProductDesignerDraftResponse(
        Long id,
        String productName,
        String businessType,
        String domesticInternational,
        String province,
        String city,
        String district,
        String receptionStandard,
        String productTheme,
        Integer travelDays,
        String designStatus,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将草稿实体转换为接口响应。 */
    public static ProductDesignerDraftResponse fromEntity(SalesProductEntity entity) {
        return new ProductDesignerDraftResponse(
                entity.getId(),
                entity.getProductName(),
                entity.getBusinessType(),
                entity.getDomesticInternational(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getReceptionStandard(),
                entity.getProductTheme(),
                entity.getTravelDays(),
                "designing",
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
