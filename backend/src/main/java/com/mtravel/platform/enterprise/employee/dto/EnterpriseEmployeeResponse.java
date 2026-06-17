package com.mtravel.platform.enterprise.employee.dto;

import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import java.time.OffsetDateTime;

/**
 * 企业员工返回对象。
 *
 * <p>员工列表和详情共用该对象。部门名称、角色名称和角色编码由 Service 查询补充，方便前端直接展示。</p>
 */
public record EnterpriseEmployeeResponse(
        Long id,
        Long systemUserId,
        String employeeCode,
        String employeeName,
        String username,
        Long departmentId,
        String departmentName,
        Long roleId,
        String roleCode,
        String roleName,
        String gender,
        String telephone,
        String mobilePhone,
        String email,
        String infoScope,
        String profitScope,
        String receptionScope,
        String customerScope,
        Integer sortOrder,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将员工实体转换为接口返回对象。 */
    public static EnterpriseEmployeeResponse fromEntity(
            EnterpriseEmployeeEntity entity,
            String departmentName,
            String roleCode,
            String roleName
    ) {
        return new EnterpriseEmployeeResponse(
                entity.getId(),
                entity.getSystemUserId(),
                entity.getEmployeeCode(),
                entity.getEmployeeName(),
                entity.getUsername(),
                entity.getDepartmentId(),
                departmentName,
                entity.getRoleId(),
                roleCode,
                roleName,
                entity.getGender(),
                entity.getTelephone(),
                entity.getMobilePhone(),
                entity.getEmail(),
                entity.getInfoScope(),
                entity.getProfitScope(),
                entity.getReceptionScope(),
                entity.getCustomerScope(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
