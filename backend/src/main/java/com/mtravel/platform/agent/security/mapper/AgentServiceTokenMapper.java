package com.mtravel.platform.agent.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.agent.security.entity.AgentServiceTokenEntity;
import org.apache.ibatis.annotations.Mapper;

/** Agent 服务令牌数据库访问。 */
@Mapper
public interface AgentServiceTokenMapper extends BaseMapper<AgentServiceTokenEntity> {
}
