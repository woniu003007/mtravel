package com.mtravel.platform.enterprise.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;

/**
 * 企业导游档案数据库访问 Mapper。
 *
 * <p>导游管理当前使用 MyBatis-Plus 基础 CRUD 能力，租户边界、查重、软删除和状态规则由 Service 统一处理。</p>
 */
public interface EnterpriseGuideMapper extends BaseMapper<EnterpriseGuideEntity> {
}
