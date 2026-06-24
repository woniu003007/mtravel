package com.mtravel.platform.sales.booking.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 收客订单价格明细实体，对应 sales_order_price_lines 表。
 *
 * <p>价格明细用于保存成人、儿童、房差、票务等报价组成。订单主表只保存合计金额，
 * 明细行用于打印确认单、结算单以及后续费用核对。</p>
 */
@TableName("sales_order_price_lines")
public class SalesBookingOrderPriceLineEntity extends TenantSoftDeleteEntity {

    /** 所属订单 ID。 */
    @TableField("order_id")
    private Long orderId;

    /** 所属团队 ID，冗余用于团队级查询和统计。 */
    @TableField("team_id")
    private Long teamId;

    /** 明细类型，例如 adult、child、ticket、misc。 */
    @TableField("line_type")
    private String lineType;

    /** 明细名称，面向业务展示。 */
    @TableField("item_name")
    private String itemName;

    /** 单价。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 数量。 */
    @TableField("quantity")
    private BigDecimal quantity;

    /** 小计金额。 */
    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;

    /** 排序号，保持页面录入顺序。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
