package com.mtravel.platform.sales.team.documentimport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportApplyRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/** 团队文档导入幂等记录数据访问。 */
@Mapper
public interface SalesDocumentImportApplyRecordMapper extends BaseMapper<SalesDocumentImportApplyRecordEntity> {
}
