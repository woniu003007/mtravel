package com.mtravel.platform.sales.booking.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 收客订单收入明细实体，对应 sales_order_charge_lines 表。
 *
 * <p>本表统一保存订单原始价格和后续应收变更。原始价格使用 base_price，费用变更使用 adjustment，
 * 真实收款、成本、付款和发票不写入本表。</p>
 */
@TableName("sales_order_charge_lines")
public class SalesBookingOrderChargeLineEntity extends TenantSoftDeleteEntity {

    /** 所属订单 ID。 */
    @TableField("order_id")
    private Long orderId;

    /** 所属团队 ID，冗余用于团队级统计。 */
    @TableField("team_id")
    private Long teamId;

    /** 收入行类型：base_price 原始价格，adjustment 应收变更。 */
    @TableField("line_kind")
    private String lineKind;

    /** 明细类型，例如 adult、surcharge、ticket。 */
    @TableField("line_type")
    private String lineType;

    /** 明细名称，面向业务展示。 */
    @TableField("item_name")
    private String itemName;

    /** 单价，原始价格行使用。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 数量，原始价格行使用。 */
    @TableField("quantity")
    private BigDecimal quantity;

    /** 是否占用团队人数名额，原始价格行使用。 */
    @TableField("occupy_seat")
    private Boolean occupySeat;

    /** 金额。原始价格为单价乘数量，应收变更按正负号保存。 */
    @TableField("amount")
    private BigDecimal amount;

    /** 变更方向：increase 加收，decrease 退减，仅 adjustment 使用。 */
    @TableField("change_type")
    private String changeType;

    /** 费用项目 ID，仅 adjustment 使用。 */
    @TableField("fee_project_id")
    private Long feeProjectId;

    /** 费用项目名称快照，仅 adjustment 使用。 */
    @TableField("fee_project_name")
    private String feeProjectName;

    /** 费用变更说明，仅 adjustment 使用。 */
    @TableField("fee_description")
    private String feeDescription;

    /** 收入行状态：effective 生效，approved 生效变更，cancelled 作废等。 */
    @TableField("status")
    private String status;

    /** 登记人，主要用于费用变更展示。 */
    @TableField("registered_by")
    private String registeredBy;

    /** 登记时间，主要用于费用变更展示。 */
    @TableField("registered_at")
    private OffsetDateTime registeredAt;

    /** 显示顺序，原始价格行使用。 */
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

    public String getLineKind() {
        return lineKind;
    }

    public void setLineKind(String lineKind) {
        this.lineKind = lineKind;
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

    public Boolean getOccupySeat() {
        return occupySeat;
    }

    public void setOccupySeat(Boolean occupySeat) {
        this.occupySeat = occupySeat;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Long getFeeProjectId() {
        return feeProjectId;
    }

    public void setFeeProjectId(Long feeProjectId) {
        this.feeProjectId = feeProjectId;
    }

    public String getFeeProjectName() {
        return feeProjectName;
    }

    public void setFeeProjectName(String feeProjectName) {
        this.feeProjectName = feeProjectName;
    }

    public String getFeeDescription() {
        return feeDescription;
    }

    public void setFeeDescription(String feeDescription) {
        this.feeDescription = feeDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(OffsetDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
