package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品每日行程 Mapper。
 *
 * <p>用于维护产品模板下按天拆分的行程内容。</p>
 */
@Mapper
public interface SalesProductItineraryDayMapper extends BaseMapper<SalesProductItineraryDayEntity> {
}
