package com.mtravel.platform.sales.booking.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderChargeLineEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收客订单收入明细 Mapper。
 */
@Mapper
public interface SalesBookingOrderChargeLineMapper extends BaseMapper<SalesBookingOrderChargeLineEntity> {
}
