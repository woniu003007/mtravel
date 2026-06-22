package com.mtravel.platform.purchase.relation.tickettemplate.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 游客名单模板字段映射实体，对应 purchase_relation_ticket_template_fields 表。
 *
 * <p>每行表示 Excel 模板中的一列映射到系统游客资料中的哪个字段。</p>
 */
@TableName("purchase_relation_ticket_template_fields")
public class PurchaseRelationTicketTemplateFieldEntity extends TenantSoftDeleteEntity {

    /** 模板主表 ID。 */
    @TableField("template_id")
    private Long templateId;

    /** Excel 表头名称。 */
    @TableField("template_header")
    private String templateHeader;

    /** Excel 列序号，从 1 开始。 */
    @TableField("column_index")
    private Integer columnIndex;

    /** 系统游客字段编码。游客字段填充时必填，其它填充方式可为空。 */
    @TableField("system_field")
    private String systemField;

    /** 填充方式：tourist_field、sequence、constant、keep_original。 */
    @TableField("fill_mode")
    private String fillMode;

    /** 固定值填充内容。 */
    @TableField("fixed_value")
    private String fixedValue;

    /** 该列在模板中是否必填。 */
    @TableField("required")
    private Boolean required;

    /** 页面展示排序。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateHeader() { return templateHeader; }
    public void setTemplateHeader(String templateHeader) { this.templateHeader = templateHeader; }
    public Integer getColumnIndex() { return columnIndex; }
    public void setColumnIndex(Integer columnIndex) { this.columnIndex = columnIndex; }
    public String getSystemField() { return systemField; }
    public void setSystemField(String systemField) { this.systemField = systemField; }
    public String getFillMode() { return fillMode; }
    public void setFillMode(String fillMode) { this.fillMode = fillMode; }
    public String getFixedValue() { return fixedValue; }
    public void setFixedValue(String fixedValue) { this.fixedValue = fixedValue; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
