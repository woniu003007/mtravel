package com.mtravel.platform.enterprise.companyinfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.enterprise.companyinfo.entity.EnterpriseCompanyInfoEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业公司信息数据库访问接口。
 *
 * <p>公司信息按租户维护一份，具体的单记录查询、更新和租户过滤由 Service 统一处理。</p>
 */
@Mapper
public interface EnterpriseCompanyInfoMapper extends BaseMapper<EnterpriseCompanyInfoEntity> {
}
