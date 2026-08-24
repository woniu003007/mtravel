package com.mtravel.platform.enterprise.department.dto;

import com.mtravel.platform.enterprise.department.entity.EnterpriseDepartmentEntity;
import java.time.OffsetDateTime;

/**
 * 企业部门返回对象。
 *
 * <p>列表、详情和下拉选择共用该对象。`parentName` 由 Service 补充，方便前端直接展示上级部门名称。</p>
 */
public record EnterpriseDepartmentResponse(
        Long id,
        Long parentId,
        String parentName,
        String departmentCode,
        String departmentName,
        Long managerEmployeeId,
        String managerName,
        String contactPhone,
        Integer sortOrder,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将数据库实体转换为接口返回对象。 */
    public static EnterpriseDepartmentResponse fromEntity(EnterpriseDepartmentEntity entity, String parentName) {
        return new EnterpriseDepartmentResponse(
                entity.getId(),
                entity.getParentId(),
                parentName,
                entity.getDepartmentCode(),
                entity.getDepartmentName(),
                entity.getManagerEmployeeId(),
                entity.getManagerName(),
                entity.getContactPhone(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
