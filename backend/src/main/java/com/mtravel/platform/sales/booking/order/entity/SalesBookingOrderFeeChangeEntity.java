package com.mtravel.platform.sales.booking.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 收客订单费用变更实体，对应 sales_order_fee_changes 表。
 *
 * <p>费用变更是订单保存后的追加调整记录。收客页新增的费用变更立即生效，金额正负由变更方向决定；
 * 费用项目保存 ID 和名称快照，用于后续统计和历史追溯。</p>
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

    /** 费用项目 ID，来自企业资料附加费用项目。 */
    @TableField("fee_project_id")
    private Long feeProjectId;

    /** 费用项目名称快照。 */
    @TableField("fee_project_name")
    private String feeProjectName;

    /** 费用变更说明。 */
    @TableField("fee_description")
    private String feeDescription;

    /** 变更金额。加收为正数，退减为负数。 */
    @TableField("amount")
    private BigDecimal amount;

    /** 状态：pending、approved、rejected、cancelled。 */
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
