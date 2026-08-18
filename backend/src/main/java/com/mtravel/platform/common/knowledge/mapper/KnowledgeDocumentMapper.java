package com.mtravel.platform.common.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Mapper;

/** 知识文档 Mapper。 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {
}
