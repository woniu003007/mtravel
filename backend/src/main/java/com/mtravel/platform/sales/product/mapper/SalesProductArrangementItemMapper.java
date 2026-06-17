package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品团队安排参数 Mapper。
 *
 * <p>用于维护产品模板下资源与费用参考，不处理正式计调履约。</p>
 */
@Mapper
public interface SalesProductArrangementItemMapper extends BaseMapper<SalesProductArrangementItemEntity> {
}
