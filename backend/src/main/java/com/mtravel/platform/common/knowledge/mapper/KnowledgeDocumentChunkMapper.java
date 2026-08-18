package com.mtravel.platform.common.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentChunkEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 知识文档切片 Mapper。 */
@Mapper
public interface KnowledgeDocumentChunkMapper extends BaseMapper<KnowledgeDocumentChunkEntity> {

    /** 插入切片；embedding 为空时先保存文本切片，等待后续向量化重试。 */
    @Insert("""
            INSERT INTO knowledge_document_chunks (
              tenant_id, document_id, source_type, source_id, chunk_no, chunk_text,
              token_count, page_no, heading, embedding_model, embedding, index_version
            ) VALUES (
              #{tenantId}, #{documentId}, #{sourceType}, #{sourceId}, #{chunkNo}, #{chunkText},
              #{tokenCount}, #{pageNo}, #{heading}, #{embeddingModel},
              CASE WHEN #{embedding} IS NULL THEN NULL ELSE #{embedding}::vector END,
              #{indexVersion}
            )
            """)
    void insertChunk(KnowledgeDocumentChunkEntity entity);

    /** 物理删除某个文档的全部切片和向量。 */
    @Delete("""
            DELETE FROM knowledge_document_chunks
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
            """)
    int deleteByDocument(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId);
}
