package com.mtravel.platform.enterprise.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagRelationEntity;

/**
 * 导游标签关系数据库访问 Mapper。
 *
 * <p>关系表只记录导游与标签的绑定，具体标签状态和名称由导游标签表维护。</p>
 */
public interface EnterpriseGuideTagRelationMapper extends BaseMapper<EnterpriseGuideTagRelationEntity> {
}
