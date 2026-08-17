package com.mtravel.platform.purchase.resourcequote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.resourcequote.entity.ResourceQuoteRuleEntity;

/**
 * 普通资源报价规则 Mapper。
 *
 * <p>基础 CRUD 由 MyBatis-Plus 提供，资源类型、客户等级和唯一规则判断由 Service 处理。</p>
 */
public interface ResourceQuoteRuleMapper extends BaseMapper<ResourceQuoteRuleEntity> {
}
