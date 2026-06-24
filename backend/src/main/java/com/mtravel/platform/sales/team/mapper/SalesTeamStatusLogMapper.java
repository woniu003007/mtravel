package com.mtravel.platform.sales.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamStatusLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售团队状态日志 Mapper。
 *
 * <p>用于记录团期创建、停收、恢复、取消和删除等状态动作。</p>
 */
@Mapper
public interface SalesTeamStatusLogMapper extends BaseMapper<SalesTeamStatusLogEntity> {
}
