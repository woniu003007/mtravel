package com.mtravel.platform.sales.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售团队价格表 Mapper。
 *
 * <p>价格表保存团队下的客户类型价格，不直接处理团队主状态。</p>
 */
@Mapper
public interface SalesTeamPriceMapper extends BaseMapper<SalesTeamPriceEntity> {
}
