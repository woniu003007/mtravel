package com.mtravel.platform.purchase.resource.alias.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/** 采购资源别名实体，保存计调确认的文档名称映射。 */
@TableName("purchase_resource_aliases")
public class PurchaseResourceAliasEntity extends TenantSoftDeleteEntity {
    @TableField("resource_id") private Long resourceId;
    @TableField("alias_name") private String aliasName;
    @TableField("normalized_alias") private String normalizedAlias;
    @TableField("source") private String source;
    @TableField("status") private String status;
    @TableField("confirmed_by") private String confirmedBy;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long value) { resourceId = value; }
    public String getAliasName() { return aliasName; }
    public void setAliasName(String value) { aliasName = value; }
    public String getNormalizedAlias() { return normalizedAlias; }
    public void setNormalizedAlias(String value) { normalizedAlias = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String value) { confirmedBy = value; }
}
