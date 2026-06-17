package com.mtravel.platform.enterprise.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.department.entity.EnterpriseDepartmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业部门数据库访问接口。
 *
 * <p>基础增删改查由 MyBatis-Plus 提供，业务筛选和树关系判断放在 Service 中。</p>
 */
@Mapper
public interface EnterpriseDepartmentMapper extends BaseMapper<EnterpriseDepartmentEntity> {
}
