package com.mtravel.platform.enterprise.bankaccount.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.bankaccount.entity.EnterpriseBankAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业银行账号数据库访问接口。
 *
 * <p>首版使用 MyBatis-Plus 基础 CRUD 能力。复杂查询由 Service 组装条件，Mapper 不承载业务判断。</p>
 */
@Mapper
public interface EnterpriseBankAccountMapper extends BaseMapper<EnterpriseBankAccountEntity> {
}
