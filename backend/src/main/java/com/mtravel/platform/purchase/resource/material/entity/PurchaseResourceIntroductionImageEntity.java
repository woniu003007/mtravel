package com.mtravel.platform.purchase.resource.material.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/** 介绍素材选用资源图片的关联记录，图片文件仍由资源图片素材库统一管理。 */
@TableName("purchase_resource_introduction_images")
public class PurchaseResourceIntroductionImageEntity extends TenantSoftDeleteEntity {

    @TableField("introduction_id")
    private Long introductionId;

    @TableField("resource_image_id")
    private Long resourceImageId;

    @TableField("sort_order")
    private Integer sortOrder;

    public Long getIntroductionId() { return introductionId; }
    public void setIntroductionId(Long introductionId) { this.introductionId = introductionId; }
    public Long getResourceImageId() { return resourceImageId; }
    public void setResourceImageId(Long resourceImageId) { this.resourceImageId = resourceImageId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
