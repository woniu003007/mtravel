package com.mtravel.platform.sales.ordertransfer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/**
 * 销售订单团队流转日志实体，对应 sales_order_transfer_logs 表。
 *
 * <p>拼团和转团都会改变订单与团队的业务关系，本表用于保留来源订单、原团队、
 * 目标团队和生成子订单之间的一一对应关系。</p>
 */
@TableName("sales_order_transfer_logs")
public class SalesOrderTransferLogEntity extends TenantSoftDeleteEntity {

    /** 来源订单 ID。 */
    @TableField("source_order_id")
    private Long sourceOrderId;

    /** 来源团队 ID。 */
    @TableField("source_team_id")
    private Long sourceTeamId;

    /** 目标团队 ID。 */
    @TableField("target_team_id")
    private Long targetTeamId;

    /** 目标团队下生成或迁入的订单 ID。 */
    @TableField("child_order_id")
    private Long childOrderId;

    /** 流转类型：merge 拼团，move 转团。 */
    @TableField("transfer_type")
    private String transferType;

    /** 流转状态：completed 已完成，cancelled 已取消。 */
    @TableField("transfer_status")
    private String transferStatus;

    /** 是否打标。 */
    @TableField("tag_flag")
    private Boolean tagFlag;

    /** 操作人。 */
    @TableField("operator")
    private String operator;

    /** 操作时间。 */
    @TableField("operated_at")
    private OffsetDateTime operatedAt;

    public Long getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(Long sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public Long getSourceTeamId() {
        return sourceTeamId;
    }

    public void setSourceTeamId(Long sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
    }

    public Long getTargetTeamId() {
        return targetTeamId;
    }

    public void setTargetTeamId(Long targetTeamId) {
        this.targetTeamId = targetTeamId;
    }

    public Long getChildOrderId() {
        return childOrderId;
    }

    public void setChildOrderId(Long childOrderId) {
        this.childOrderId = childOrderId;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getTransferStatus() {
        return transferStatus;
    }

    public void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    public Boolean getTagFlag() {
        return tagFlag;
    }

    public void setTagFlag(Boolean tagFlag) {
        this.tagFlag = tagFlag;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public OffsetDateTime getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(OffsetDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }
}
