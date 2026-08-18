package com.mtravel.platform.customer.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalStepEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 客户授信超额审批步骤数据访问 Mapper。 */
public interface CustomerRiskApprovalStepMapper extends BaseMapper<CustomerRiskApprovalStepEntity> {

    /** 在审批事务中锁定当前步骤，防止重复点击导致重复流转。 */
    @Select("""
            SELECT *
            FROM customer_risk_approval_steps
            WHERE tenant_id = #{tenantId}
              AND request_id = #{requestId}
              AND step_order = #{stepOrder}
            FOR UPDATE
            """)
    CustomerRiskApprovalStepEntity selectForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("requestId") Long requestId,
            @Param("stepOrder") Integer stepOrder
    );
}
