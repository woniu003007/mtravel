package com.mtravel.platform.enterprise.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagEntity;

/**
 * 导游标签数据库访问 Mapper。
 *
 * <p>标签管理使用 MyBatis-Plus 基础 CRUD 能力，租户隔离、软删除和名称唯一性由 Service 处理。</p>
 */
public interface EnterpriseGuideTagMapper extends BaseMapper<EnterpriseGuideTagEntity> {
}
