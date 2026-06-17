package com.mtravel.platform.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.contract.entity.ContractEntity;
import org.apache.ibatis.annotations.Mapper;

/** 统一合同数据访问接口。 */
@Mapper
public interface ContractMapper extends BaseMapper<ContractEntity> {
}
