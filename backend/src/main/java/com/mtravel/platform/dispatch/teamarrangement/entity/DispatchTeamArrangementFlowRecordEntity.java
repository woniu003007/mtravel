package com.mtravel.platform.dispatch.teamarrangement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 正式团队安排下游流程流水实体。
 *
 * <p>用于记录由团队安排成本同步生成的导游报账、计调审核等流程节点，支撑删除锁定和进度统计。</p>
 */
@TableName("dispatch_team_arrangement_flow_records")
public class DispatchTeamArrangementFlowRecordEntity extends TenantSoftDeleteEntity {

    /** 所属安排 ID。 */
    @TableField("arrangement_id")
    private Long arrangementId;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团队编号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 流程类型。 */
    @TableField("flow_type")
    private String flowType;

    /** 同步来源。 */
    @TableField("sync_source")
    private String syncSource;

    /** 流程状态。 */
    @TableField("flow_status")
    private String flowStatus;

    /** 流程金额快照。 */
    @TableField("flow_amount")
    private BigDecimal flowAmount;

    /** 登记人。 */
    @TableField("registered_by")
    private String registeredBy;

    /** 登记时间。 */
    @TableField("registered_at")
    private OffsetDateTime registeredAt;

    public Long getArrangementId() { return arrangementId; }
    public void setArrangementId(Long arrangementId) { this.arrangementId = arrangementId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }
    public String getSyncSource() { return syncSource; }
    public void setSyncSource(String syncSource) { this.syncSource = syncSource; }
    public String getFlowStatus() { return flowStatus; }
    public void setFlowStatus(String flowStatus) { this.flowStatus = flowStatus; }
    public BigDecimal getFlowAmount() { return flowAmount; }
    public void setFlowAmount(BigDecimal flowAmount) { this.flowAmount = flowAmount; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(OffsetDateTime registeredAt) { this.registeredAt = registeredAt; }
}
