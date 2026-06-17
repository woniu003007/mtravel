package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品说明 Mapper。
 *
 * <p>用于维护收客须知、费用包含不含等产品模板说明。</p>
 */
@Mapper
public interface SalesProductDescriptionMapper extends BaseMapper<SalesProductDescriptionEntity> {
}
