package com.mtravel.platform.purchase.resource.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionChunkEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 采购资源介绍素材向量切片 Mapper。 */
@Mapper
public interface PurchaseResourceIntroductionChunkMapper extends BaseMapper<PurchaseResourceIntroductionChunkEntity> {

    /** 插入介绍正文切片；向量服务未配置时允许先保存文本切片。 */
    @Insert("""
            INSERT INTO purchase_resource_introduction_chunks (
              tenant_id, introduction_id, resource_id, chunk_no, chunk_text,
              token_count, embedding_model, embedding, index_version
            ) VALUES (
              #{tenantId}, #{introductionId}, #{resourceId}, #{chunkNo}, #{chunkText},
              #{tokenCount}, #{embeddingModel},
              CASE WHEN #{embedding} IS NULL THEN NULL ELSE #{embedding}::vector END,
              #{indexVersion}
            )
            """)
    void insertChunk(PurchaseResourceIntroductionChunkEntity entity);

    /** 物理删除介绍素材的全部文本切片和向量。 */
    @Delete("""
            DELETE FROM purchase_resource_introduction_chunks
            WHERE tenant_id = #{tenantId}
              AND introduction_id = #{introductionId}
            """)
    int deleteByIntroduction(@Param("tenantId") Long tenantId, @Param("introductionId") Long introductionId);
}
