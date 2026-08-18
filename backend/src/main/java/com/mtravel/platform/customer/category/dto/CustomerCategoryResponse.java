package com.mtravel.platform.customer.category.dto;

import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 客户分类返回对象。
 *
 * <p>接口返回时不暴露 tenant_id、is_deleted、deleted_at、deleted_by，
 * 这些字段属于系统隔离和软删除控制字段，不应该成为前端业务表单内容。</p>
 */
public record CustomerCategoryResponse(
        Long id,
        String categoryName,
        BigDecimal defaultCreditLimit,
        Integer creditTermDays,
        Boolean allowOverLimit,
        List<CustomerCategoryApprovalMemberResponse> approvers,
        List<CustomerCategoryApprovalMemberResponse> ccUsers,
        Integer sortOrder,
        String status,
        String createdBy,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CustomerCategoryResponse fromEntity(
            CustomerCategoryEntity entity,
            List<CustomerCategoryApprovalMemberResponse> approvers,
            List<CustomerCategoryApprovalMemberResponse> ccUsers
    ) {
        return new CustomerCategoryResponse(
                entity.getId(),
                entity.getCategoryName(),
                entity.getDefaultCreditLimit(),
                entity.getCreditTermDays(),
                entity.getAllowOverLimit(),
                approvers,
                ccUsers,
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getRemark(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /** 兼容不需要审批人员明细的内部调用。 */
    public static CustomerCategoryResponse fromEntity(CustomerCategoryEntity entity) {
        return fromEntity(entity, List.of(), List.of());
    }
}
