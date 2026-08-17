package com.mtravel.platform.customer.creditrule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.customer.creditrule.entity.CustomerCreditRuleEntity;

/**
 * 客户授信规则 Mapper。
 *
 * <p>基础增删改查由 MyBatis-Plus 提供；等级、员工有效性和规则唯一性由 Service 负责。</p>
 */
public interface CustomerCreditRuleMapper extends BaseMapper<CustomerCreditRuleEntity> {
}
