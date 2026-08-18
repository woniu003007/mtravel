package com.mtravel.platform.agent.policy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.agent.policy.entity.AgentBusinessPolicyEntity;
import org.apache.ibatis.annotations.Mapper;

/** Agent 结构化业务政策数据访问接口。 */
@Mapper
public interface AgentBusinessPolicyMapper extends BaseMapper<AgentBusinessPolicyEntity> {
}
