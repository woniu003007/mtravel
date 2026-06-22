package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductVehicleInquiryEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 销售产品用车询价记录响应对象。
 *
 * <p>用于产品团队安排页回显多家车队报价、包含项和选定结果。</p>
 */
public record SalesProductVehicleInquiryResponse(
        Long id,
        Integer sortOrder,
        String inquiryMethod,
        String inquiryPerson,
        OffsetDateTime inquiryTime,
        String groupName,
        Long supplierId,
        String supplierName,
        BigDecimal quotedAmount,
        Boolean includesToll,
        Boolean includesParking,
        Boolean includesDriverMeal,
        Boolean includesDriverLodging,
        Integer availableVehicleCount,
        String replyPerson,
        OffsetDateTime replyTime,
        Long attachmentId,
        String attachmentUrl,
        Boolean selected,
        String remark
) {
    /** 将询价实体转换为接口响应。 */
    public static SalesProductVehicleInquiryResponse fromEntity(SalesProductVehicleInquiryEntity entity) {
        return new SalesProductVehicleInquiryResponse(
                entity.getId(),
                entity.getSortOrder(),
                entity.getInquiryMethod(),
                entity.getInquiryPerson(),
                entity.getInquiryTime(),
                entity.getGroupName(),
                entity.getSupplierId(),
                entity.getSupplierName(),
                entity.getQuotedAmount(),
                entity.getIncludesToll(),
                entity.getIncludesParking(),
                entity.getIncludesDriverMeal(),
                entity.getIncludesDriverLodging(),
                entity.getAvailableVehicleCount(),
                entity.getReplyPerson(),
                entity.getReplyTime(),
                entity.getAttachmentId(),
                entity.getAttachmentUrl(),
                entity.getSelected(),
                entity.getRemark()
        );
    }
}
