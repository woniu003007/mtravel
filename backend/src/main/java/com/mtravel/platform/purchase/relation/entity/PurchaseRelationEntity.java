package com.mtravel.platform.purchase.relation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购关系实体，对应 purchase_relations 表。
 *
 * <p>采购关系表达资源与供应商的绑定、成团数量和统一报价。分类报价的具体门市、同行、
 * 团队价格由 supplier_resource_prices 表维护。</p>
 */
@TableName("purchase_relations")
public class PurchaseRelationEntity extends TenantSoftDeleteEntity {
    /** 资源类型，从绑定资源主档带出。 */
    @TableField("resource_type")
    private String resourceType;

    /** 资源 ID，关联采购资源主档。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 资源名称快照。 */
    @TableField("resource_name")
    private String resourceName;

    /** 供应商 ID。 */
    @TableField("supplier_id")
    private Long supplierId;

    /** 成团数量。0 表示散团同价。 */
    @TableField("group_quantity")
    private Integer groupQuantity;

    /** 历史兼容采购价格字段，当前保存关系时不再写入。 */
    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    /** 历史兼容价格单位字段。 */
    @TableField("price_unit")
    private String priceUnit;

    /** 历史兼容结算方式字段。 */
    @TableField("settlement_method")
    private String settlementMethod;

    /** 历史兼容价格有效期开始日期。 */
    @TableField("valid_from")
    private LocalDate validFrom;

    /** 历史兼容价格有效期结束日期。 */
    @TableField("valid_to")
    private LocalDate validTo;

    /** 历史兼容优先级字段。 */
    @TableField("priority_level")
    private Integer priorityLevel;

    /** 是否为当前资源的默认供应商，用于景区资源页和后续业务默认带出。 */
    @TableField("is_default")
    private Boolean isDefault;

    /** 报价模式：unified 统一报价，classified 分类报价。历史关系为空时由资源页按明细兼容展示。 */
    @TableField("price_mode")
    private String priceMode;

    /** 统一报价金额，仅 price_mode=unified 时使用。 */
    @TableField("unified_price")
    private BigDecimal unifiedPrice;

    /** 统一报价的适用条件或补充说明。 */
    @TableField("price_remark")
    private String priceRemark;

    /** 状态：active 有效，disabled 停用，expired 兼容历史过期状态。 */
    @TableField("status")
    private String status;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Integer getGroupQuantity() { return groupQuantity; }
    public void setGroupQuantity(Integer groupQuantity) { this.groupQuantity = groupQuantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public String getSettlementMethod() { return settlementMethod; }
    public void setSettlementMethod(String settlementMethod) { this.settlementMethod = settlementMethod; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Integer getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public String getPriceMode() { return priceMode; }
    public void setPriceMode(String priceMode) { this.priceMode = priceMode; }
    public BigDecimal getUnifiedPrice() { return unifiedPrice; }
    public void setUnifiedPrice(BigDecimal unifiedPrice) { this.unifiedPrice = unifiedPrice; }
    public String getPriceRemark() { return priceRemark; }
    public void setPriceRemark(String priceRemark) { this.priceRemark = priceRemark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
