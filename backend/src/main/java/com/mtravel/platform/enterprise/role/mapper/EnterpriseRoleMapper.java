package com.mtravel.platform.enterprise.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.role.entity.EnterpriseRoleEntity;

/**
 * 企业角色数据库访问 Mapper。
 *
 * <p>角色管理首版使用 MyBatis-Plus 基础 CRUD 能力，复杂权限查询放在 Service 中组合完成。</p>
 */
public interface EnterpriseRoleMapper extends BaseMapper<EnterpriseRoleEntity> {
}
