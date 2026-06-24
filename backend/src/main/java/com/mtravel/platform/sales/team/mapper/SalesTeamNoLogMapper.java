package com.mtravel.platform.sales.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamNoLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售团队团号生成日志 Mapper。
 *
 * <p>团号日志只记录生成过程，不参与团队状态流转。</p>
 */
@Mapper
public interface SalesTeamNoLogMapper extends BaseMapper<SalesTeamNoLogEntity> {
}
