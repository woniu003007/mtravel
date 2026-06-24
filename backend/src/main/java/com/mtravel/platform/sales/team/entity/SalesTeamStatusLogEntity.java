package com.mtravel.platform.sales.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 销售团队状态日志实体，对应 sales_team_status_logs 表。
 *
 * <p>团队状态变化都写日志，便于后续追溯谁执行了停收、取消、恢复或删除。</p>
 */
@TableName("sales_team_status_logs")
public class SalesTeamStatusLogEntity {

    /** 日志主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 变更前状态。 */
    @TableField("from_status")
    private String fromStatus;

    /** 变更后状态。 */
    @TableField("to_status")
    private String toStatus;

    /** 状态动作类型。 */
    @TableField("action_type")
    private String actionType;

    /** 操作人。 */
    @TableField("operator")
    private String operator;

    /** 操作时间。 */
    @TableField("action_time")
    private OffsetDateTime actionTime;

    /** 变更备注。 */
    @TableField("remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public OffsetDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(OffsetDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
