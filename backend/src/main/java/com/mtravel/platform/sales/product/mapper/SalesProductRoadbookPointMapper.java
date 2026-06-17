package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductRoadbookPointEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品每日路书地点 Mapper。
 *
 * <p>用于保存和查询产品模板中每天的地图路线点位。</p>
 */
@Mapper
public interface SalesProductRoadbookPointMapper extends BaseMapper<SalesProductRoadbookPointEntity> {
}
