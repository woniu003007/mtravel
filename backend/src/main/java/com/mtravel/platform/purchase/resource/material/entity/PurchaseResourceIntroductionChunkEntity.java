package com.mtravel.platform.purchase.resource.material.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/** 资源介绍素材向量切片实体，对应 purchase_resource_introduction_chunks 表。 */
@TableName("purchase_resource_introduction_chunks")
public class PurchaseResourceIntroductionChunkEntity {

    @TableField("id")
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("introduction_id")
    private Long introductionId;
    @TableField("resource_id")
    private Long resourceId;
    @TableField("chunk_no")
    private Integer chunkNo;
    @TableField("chunk_text")
    private String chunkText;
    @TableField("token_count")
    private Integer tokenCount;
    @TableField("embedding_model")
    private String embeddingModel;
    @TableField("embedding")
    private String embedding;
    @TableField("index_version")
    private Integer indexVersion;
    @TableField("created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getIntroductionId() { return introductionId; }
    public void setIntroductionId(Long introductionId) { this.introductionId = introductionId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Integer getIndexVersion() { return indexVersion; }
    public void setIndexVersion(Integer indexVersion) { this.indexVersion = indexVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
