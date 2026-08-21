package com.mtravel.platform.purchase.relation.optional.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 采购关系自费项目报价实体。
 *
 * <p>自费项目是资源行程中可选的门票或体验项目，报价属于某一条资源供应商采购关系，
 * 不混入普通成人、儿童等费用项目报价。金额统一按元/人保存。</p>
 */
@TableName("purchase_relation_optional_items")
public class PurchaseRelationOptionalItemEntity extends TenantSoftDeleteEntity {

    /** 资源与供应商绑定关系 ID。 */
    @TableField("relation_id")
    private Long relationId;

    /** 资源级自费项目主档 ID；历史未迁移记录允许为空。 */
    @TableField("resource_optional_item_id")
    private Long resourceOptionalItemId;

    /** 自费项目名称，例如苏州游船。 */
    @TableField("project_name")
    private String projectName;

    /** 供应商成本价，统一按元/人。 */
    @TableField("cost_price")
    private BigDecimal costPrice;

    /** 供应商建议游客对外价，仅作为产品设计的默认值。 */
    @TableField("suggested_sale_price")
    private BigDecimal suggestedSalePrice;

    /** 计价单位代码，当前固定为 yuan_per_person。 */
    @TableField("price_unit")
    private String priceUnit;

    /** 自费项目价格说明。 */
    @TableField("price_description")
    private String priceDescription;

    /** 状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }
    public Long getResourceOptionalItemId() { return resourceOptionalItemId; }
    public void setResourceOptionalItemId(Long resourceOptionalItemId) { this.resourceOptionalItemId = resourceOptionalItemId; }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
    public BigDecimal getSuggestedSalePrice() { return suggestedSalePrice; }
    public void setSuggestedSalePrice(BigDecimal suggestedSalePrice) { this.suggestedSalePrice = suggestedSalePrice; }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public String getPriceDescription() {
        return priceDescription;
    }

    public void setPriceDescription(String priceDescription) {
        this.priceDescription = priceDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
