package com.mtravel.platform.purchase.resource.material.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import com.mtravel.platform.purchase.resource.material.typehandler.PostgreSqlJsonbStringTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** 采购资源图片素材实体，对应 purchase_resource_images 表。 */
@TableName(value = "purchase_resource_images", autoResultMap = true)
public class PurchaseResourceImageEntity extends TenantSoftDeleteEntity {

    /** 资源主档 ID。 */
    @TableField("resource_id")
    private Long resourceId;
    /** 公共附件 ID。 */
    @TableField("attachment_id")
    private Long attachmentId;
    /** 原始文件名快照。 */
    @TableField("original_filename")
    private String originalFilename;
    /** 文件扩展名。 */
    @TableField("file_ext")
    private String fileExt;
    /** 文件大小，单位字节。 */
    @TableField("file_size")
    private Long fileSize;
    /** JSON 数组格式的图片标签。 */
    @TableField(value = "tags", typeHandler = PostgreSqlJsonbStringTypeHandler.class, jdbcType = JdbcType.OTHER)
    private String tags;
    /** 是否为当前资源封面图。 */
    @TableField("is_cover")
    private Boolean isCover;
    /** 展示排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;
    /** 图片状态。 */
    @TableField("status")
    private String status;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Boolean getIsCover() { return isCover; }
    public void setIsCover(Boolean isCover) { this.isCover = isCover; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
