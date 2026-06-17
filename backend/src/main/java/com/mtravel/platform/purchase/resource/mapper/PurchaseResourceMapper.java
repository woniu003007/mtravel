package com.mtravel.platform.purchase.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;

/**
 * 采购资源 Mapper。
 *
 * <p>基础增删改查由 MyBatis-Plus 提供，资源绑定数量由 Service 通过采购关系表计算。</p>
 */
public interface PurchaseResourceMapper extends BaseMapper<PurchaseResourceEntity> {
}
