package com.mtravel.platform.sales.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售团队主表 Mapper。
 *
 * <p>只负责 sales_teams 表访问，团期生成、状态流转和删除规则由 Service 处理。</p>
 */
@Mapper
public interface SalesTeamMapper extends BaseMapper<SalesTeamEntity> {
}
