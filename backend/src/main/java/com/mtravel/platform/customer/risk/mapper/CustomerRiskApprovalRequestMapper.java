package com.mtravel.platform.customer.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;

/**
 * 客户风控审批申请 Mapper。
 *
 * <p>基础增删改查由 MyBatis-Plus 提供，服务层负责租户边界、状态流转和审批权限校验。</p>
 */
public interface CustomerRiskApprovalRequestMapper extends BaseMapper<CustomerRiskApprovalRequestEntity> {
}
