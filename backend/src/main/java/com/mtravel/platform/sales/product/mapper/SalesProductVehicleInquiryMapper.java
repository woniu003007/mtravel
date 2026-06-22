package com.mtravel.platform.sales.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.product.entity.SalesProductVehicleInquiryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售产品用车询价记录 Mapper。
 *
 * <p>用于随产品团队安排保存和回显多家车队询价记录。</p>
 */
@Mapper
public interface SalesProductVehicleInquiryMapper extends BaseMapper<SalesProductVehicleInquiryEntity> {
}
