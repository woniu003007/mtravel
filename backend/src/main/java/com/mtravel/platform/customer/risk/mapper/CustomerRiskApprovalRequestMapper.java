package com.mtravel.platform.customer.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 客户风控审批申请 Mapper。
 *
 * <p>基础增删改查由 MyBatis-Plus 提供，服务层负责租户边界、状态流转和审批权限校验。</p>
 */
public interface CustomerRiskApprovalRequestMapper extends BaseMapper<CustomerRiskApprovalRequestEntity> {

    /** 在审批事务中锁定申请主单，保证当前步骤只流转一次。 */
    @Select("""
            SELECT *
            FROM customer_risk_approval_requests
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND is_deleted = false
            FOR UPDATE
            """)
    CustomerRiskApprovalRequestEntity selectForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id
    );

    /**
     * 获取客户风控审批申请编号生成的事务级数据库锁。
     *
     * @return PostgreSQL void 锁函数的文本占位结果，业务层只依赖加锁副作用
     */
    @Select("""
            SELECT pg_advisory_xact_lock(hashtextextended(CONCAT('customer_risk_approval_no:', #{tenantId}, ':', #{prefix}), 0))::text
            """)
    String lockRequestNoGeneration(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);

    /**
     * 查询当前审批申请编号前缀下的最大数字后缀。
     */
    @Select("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(request_no FROM LENGTH(#{prefix}) + 1) AS integer)), 0)
            FROM customer_risk_approval_requests
            WHERE tenant_id = #{tenantId}
              AND is_deleted = false
              AND request_no LIKE CONCAT(#{prefix}, '%')
              AND SUBSTRING(request_no FROM LENGTH(#{prefix}) + 1) ~ '^[0-9]+$'
            """)
    Integer maxRequestNoSuffix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);
}
