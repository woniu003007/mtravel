package com.mtravel.platform.sales.booking.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收客订单游客名单 Mapper。
 */
@Mapper
public interface SalesBookingOrderGuestMapper extends BaseMapper<SalesBookingOrderGuestEntity> {
}
