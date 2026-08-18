package com.mtravel.platform.sales.team.documentimport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/** 团队文档导入任务数据访问。 */
@Mapper
public interface SalesDocumentImportTaskMapper extends BaseMapper<SalesDocumentImportTaskEntity> {
}
