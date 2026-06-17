package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品模板主表 Mapper。
 *
 * <p>只负责 sales_products 表访问，产品保存的业务规则由 SalesProductService 处理。</p>
 */
@Mapper
public interface SalesProductMapper extends BaseMapper<SalesProductEntity> {
}
