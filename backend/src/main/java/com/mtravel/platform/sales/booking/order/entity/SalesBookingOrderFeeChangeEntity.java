package com.mtravel.platform.sales.booking.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 收客订单费用变更实体，对应 sales_order_fee_changes 表。
 *
 * <p>费用变更是订单保存后的追加调整记录，后续由费用变更管理审核。本次收客订单页面先读取展示，
 * 不在订单保存接口里直接生成审批记录。</p>
 */
@TableName("sales_order_fee_changes")
public class SalesBookingOrderFeeChangeEntity extends TenantSoftDeleteEntity {

    /** 所属订单 ID。 */
    @TableField("order_id")
    private Long orderId;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 变更类型：increase 增加，decrease 减少。 */
    @TableField("change_type")
    private String changeType;

    /** 费用变更说明。 */
    @TableField("fee_description")
    private String feeDescription;

    /** 变更金额。 */
    @TableField("amount")
    private BigDecimal amount;

    /** 审核状态：pending、approved、rejected。 */
    @TableField("status")
    private String status;

    /** 登记人。 */
    @TableField("registered_by")
    private String registeredBy;

    /** 登记时间。 */
    @TableField("registered_at")
    private OffsetDateTime registeredAt;

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

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getFeeDescription() {
        return feeDescription;
    }

    public void setFeeDescription(String feeDescription) {
        this.feeDescription = feeDescription;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
}
