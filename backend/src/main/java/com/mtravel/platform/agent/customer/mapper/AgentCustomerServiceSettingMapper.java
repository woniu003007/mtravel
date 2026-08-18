package com.mtravel.platform.agent.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.agent.customer.entity.AgentCustomerServiceSettingEntity;
import org.apache.ibatis.annotations.Mapper;

/** 客户 Agent 服务能力配置数据库访问。 */
@Mapper
public interface AgentCustomerServiceSettingMapper extends BaseMapper<AgentCustomerServiceSettingEntity> {
}
