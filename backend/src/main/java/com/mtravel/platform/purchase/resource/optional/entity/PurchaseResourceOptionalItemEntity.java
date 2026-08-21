package com.mtravel.platform.purchase.resource.optional.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/** 景区资源下可复用的自费项目主档，不保存任何供应商成本。 */
@TableName("purchase_resource_optional_items")
public class PurchaseResourceOptionalItemEntity extends TenantSoftDeleteEntity {
    @TableField("resource_id") private Long resourceId;
    @TableField("project_name") private String projectName;
    @TableField("item_type") private String itemType;
    @TableField("price_unit") private String priceUnit;
    @TableField("status") private String status;
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
