package com.mtravel.platform.customer.unit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;

/**
 * 客户单位 MyBatis-Plus Mapper。
 *
 * <p>首版使用 BaseMapper 提供基础 CRUD。跨表展示客户分类名称时，
 * Service 侧批量读取分类并组装，避免为简单列表提前引入复杂 XML SQL。</p>
 */
public interface CustomerUnitMapper extends BaseMapper<CustomerUnitEntity> {
}
