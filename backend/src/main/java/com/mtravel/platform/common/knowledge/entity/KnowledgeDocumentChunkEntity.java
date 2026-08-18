package com.mtravel.platform.common.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 知识文档切片实体，对应 knowledge_document_chunks 表。
 *
 * <p>切片表保存可检索文本和 pgvector 向量。删除业务资料时本表按文档 ID 物理删除，
 * 确保被删除资料不会继续参与问答检索。</p>
 */
@TableName("knowledge_document_chunks")
public class KnowledgeDocumentChunkEntity {

    /** 切片主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 知识文档 ID。 */
    @TableField("document_id")
    private Long documentId;

    /** 业务来源类型。 */
    @TableField("source_type")
    private String sourceType;

    /** 业务来源记录 ID。 */
    @TableField("source_id")
    private Long sourceId;

    /** 切片序号，从 1 开始。 */
    @TableField("chunk_no")
    private Integer chunkNo;

    /** 切片正文。 */
    @TableField("chunk_text")
    private String chunkText;

    /** 估算 token 数。 */
    @TableField("token_count")
    private Integer tokenCount;

    /** 来源页码。 */
    @TableField("page_no")
    private Integer pageNo;

    /** 来源标题或章节名。 */
    @TableField("heading")
    private String heading;

    /** 向量模型名称。 */
    @TableField("embedding_model")
    private String embeddingModel;

    /** pgvector 字面量，例如 [0.1,0.2]。 */
    @TableField("embedding")
    private String embedding;

    /** 索引版本号。 */
    @TableField("index_version")
    private Integer indexVersion;

    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Integer getIndexVersion() { return indexVersion; }
    public void setIndexVersion(Integer indexVersion) { this.indexVersion = indexVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
