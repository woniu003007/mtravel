package com.mtravel.platform.enterprise.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;

/**
 * 企业员工数据库访问 Mapper。
 *
 * <p>员工管理使用 MyBatis-Plus 基础 CRUD 能力，账号联动规则放在 Service 中统一处理。</p>
 */
public interface EnterpriseEmployeeMapper extends BaseMapper<EnterpriseEmployeeEntity> {
}
