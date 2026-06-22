package com.mtravel.platform.dispatch.vehicleusage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.dispatch.vehicleusage.entity.VehicleUsageHistoryEntity;

/**
 * 用车历史候选 Mapper。
 *
 * <p>基础增删改查由 MyBatis-Plus 提供，查询条件在 Service 中统一加租户和软删除边界。</p>
 */
public interface VehicleUsageHistoryMapper extends BaseMapper<VehicleUsageHistoryEntity> {
}
