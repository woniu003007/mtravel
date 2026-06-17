package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementPriceLineEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品团队安排价格明细 Mapper。
 *
 * <p>用于保存产品模板中“价格信息”的多行费用项目，例如住宿房型、景区票种、购物品类等。</p>
 */
@Mapper
public interface SalesProductArrangementPriceLineMapper extends BaseMapper<SalesProductArrangementPriceLineEntity> {
}
