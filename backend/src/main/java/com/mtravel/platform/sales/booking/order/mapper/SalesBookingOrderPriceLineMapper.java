package com.mtravel.platform.sales.booking.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderPriceLineEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收客订单价格明细 Mapper。
 */
@Mapper
public interface SalesBookingOrderPriceLineMapper extends BaseMapper<SalesBookingOrderPriceLineEntity> {
}
