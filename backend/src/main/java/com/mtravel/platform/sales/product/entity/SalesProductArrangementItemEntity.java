package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售产品团队安排参数实体，对应 sales_product_arrangement_items 表。
 *
 * <p>该表保存产品模板阶段的大交通、住宿、用车、景区等默认安排和费用参考，不代表真实排团履约。</p>
 */
@TableName("sales_product_arrangement_items")
public class SalesProductArrangementItemEntity extends TenantSoftDeleteEntity {

    /** 所属产品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 安排类型，例如 hotel、vehicle、scenic。 */
    @TableField("arrangement_type")
    private String arrangementType;

    /** 安排项目名称。 */
    @TableField("item_name")
    private String itemName;

    /** 安排内容或默认说明。 */
    @TableField("arrangement_content")
    private String arrangementContent;

    /** 默认数量。 */
    @TableField("quantity")
    private BigDecimal quantity;

    /** 默认单价或费用参考。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 计量单位。 */
    @TableField("unit_name")
    private String unitName;

    /** 结算类型。cash 现结，credit 挂账。 */
    @TableField("settlement_type")
    private String settlementType;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(String arrangementType) {
        this.arrangementType = arrangementType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getArrangementContent() {
        return arrangementContent;
    }

    public void setArrangementContent(String arrangementContent) {
        this.arrangementContent = arrangementContent;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(String settlementType) {
        this.settlementType = settlementType;
    }
}
