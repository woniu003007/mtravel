package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import com.mtravel.platform.purchase.resource.material.typehandler.PostgreSqlJsonbStringTypeHandler;
import java.time.OffsetDateTime;
import org.apache.ibatis.type.JdbcType;

/** 产品生成文件版本实体，对应 sales_product_document_versions 表。 */
@TableName(value = "sales_product_document_versions", autoResultMap = true)
public class SalesProductDocumentVersionEntity extends TenantSoftDeleteEntity {

    @TableField("product_id") private Long productId;
    @TableField("document_type") private String documentType;
    @TableField("version_no") private Integer versionNo;
    @TableField(value = "source_snapshot", typeHandler = PostgreSqlJsonbStringTypeHandler.class, jdbcType = JdbcType.OTHER)
    private String sourceSnapshot;
    @TableField("attachment_id") private Long attachmentId;
    @TableField("file_name_snapshot") private String fileNameSnapshot;
    @TableField("generate_status") private String generateStatus;
    @TableField("generated_by") private String generatedBy;
    @TableField("generated_at") private OffsetDateTime generatedAt;
    @TableField("error_message") private String errorMessage;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getFileNameSnapshot() { return fileNameSnapshot; }
    public void setFileNameSnapshot(String fileNameSnapshot) { this.fileNameSnapshot = fileNameSnapshot; }
    public String getGenerateStatus() { return generateStatus; }
    public void setGenerateStatus(String generateStatus) { this.generateStatus = generateStatus; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
