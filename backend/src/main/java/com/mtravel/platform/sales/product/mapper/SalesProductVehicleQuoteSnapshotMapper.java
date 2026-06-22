package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductVehicleQuoteSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品用车报价测算快照 Mapper。
 *
 * <p>用于随产品团队安排保存和回显用车参考价快照。</p>
 */
@Mapper
public interface SalesProductVehicleQuoteSnapshotMapper extends BaseMapper<SalesProductVehicleQuoteSnapshotEntity> {
}
