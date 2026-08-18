package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/** 产品设计工作台资源配图快照实体，对应 sales_product_day_resource_images 表。 */
@TableName("sales_product_day_resource_images")
public class SalesProductDayResourceImageEntity extends TenantSoftDeleteEntity {

    @TableField("product_id") private Long productId;
    @TableField("day_resource_id") private Long dayResourceId;
    @TableField("resource_image_id") private Long resourceImageId;
    @TableField("attachment_id") private Long attachmentId;
    @TableField("original_filename_snapshot") private String originalFilenameSnapshot;
    @TableField("caption_snapshot") private String captionSnapshot;
    @TableField("sort_order") private Integer sortOrder;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getDayResourceId() { return dayResourceId; }
    public void setDayResourceId(Long dayResourceId) { this.dayResourceId = dayResourceId; }
    public Long getResourceImageId() { return resourceImageId; }
    public void setResourceImageId(Long resourceImageId) { this.resourceImageId = resourceImageId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getOriginalFilenameSnapshot() { return originalFilenameSnapshot; }
    public void setOriginalFilenameSnapshot(String originalFilenameSnapshot) { this.originalFilenameSnapshot = originalFilenameSnapshot; }
    public String getCaptionSnapshot() { return captionSnapshot; }
    public void setCaptionSnapshot(String captionSnapshot) { this.captionSnapshot = captionSnapshot; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
