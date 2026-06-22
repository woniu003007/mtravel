package com.mtravel.platform.dispatch.vehiclequote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.dispatch.vehiclequote.entity.VehicleQuoteRuleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 座位数报价规则 Mapper。
 *
 * <p>只负责 vehicle_quote_rules 表访问，报价计算规则放在 Service 中。</p>
 */
@Mapper
public interface VehicleQuoteRuleMapper extends BaseMapper<VehicleQuoteRuleEntity> {
}
