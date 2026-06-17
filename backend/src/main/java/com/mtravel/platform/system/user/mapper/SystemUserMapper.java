package com.mtravel.platform.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.system.user.entity.SystemUserEntity;

/**
 * 系统用户数据库访问 Mapper。
 *
 * <p>首版使用 MyBatis-Plus 基础 CRUD 能力，登录时按租户、账号和软删除状态查询用户。</p>
 */
public interface SystemUserMapper extends BaseMapper<SystemUserEntity> {
}
