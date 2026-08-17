package com.mtravel.platform.customer.creditrule.dto;

import com.mtravel.platform.customer.creditrule.entity.CustomerCreditRuleEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 客户授信规则响应对象。
 *
 * <p>员工 ID 和名称列表保持相同顺序，前端可直接用于审批链和抄送人回显。</p>
 */
public record CustomerCreditRuleResponse(
        Long id,
        Long customerLevelId,
        String customerLevelName,
        BigDecimal creditLimit,
        Integer paymentTermDays,
        Boolean allowOverLimit,
        List<Long> approverEmployeeIds,
        List<String> approverNames,
        List<Long> ccEmployeeIds,
        List<String> ccNames,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 将规则实体和批量查询出的展示名称转换为接口响应。 */
    public static CustomerCreditRuleResponse fromEntity(
            CustomerCreditRuleEntity entity,
            String customerLevelName,
            List<Long> approverEmployeeIds,
            List<String> approverNames,
            List<Long> ccEmployeeIds,
            List<String> ccNames
    ) {
        return new CustomerCreditRuleResponse(
                entity.getId(),
                entity.getCustomerLevelId(),
                customerLevelName,
                entity.getCreditLimit(),
                entity.getAccountPeriodDays(),
                entity.getAllowOverLimit(),
                approverEmployeeIds,
                approverNames,
                ccEmployeeIds,
                ccNames,
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
