package com.mtravel.platform.dispatch.roomstatus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;

/**
 * 自控房间每日房态实体。
 *
 * <p>该表是排房联动的核心：同一房间同一天只能有一个有效房态。锁房、排房确认、
 * 维修停用和释放都会更新这里，后续团队住宿安排从这里读取可用房源。</p>
 */
@TableName("controlled_room_day_statuses")
public class ControlledRoomDayStatusEntity extends TenantSoftDeleteEntity {

    /** 自控房源批次 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 具体房间 ID。 */
    @TableField("room_id")
    private Long roomId;

    /** 住宿日期。 */
    @TableField("stay_date")
    private LocalDate stayDate;

    /** 每日房态：available 可用，locked 已锁定，occupied 已占用，maintenance 维修，reserved 保留。 */
    @TableField("status")
    private String status;

    /** 当前锁房流水 ID，状态为 locked 或 occupied 时用于追踪来源。 */
    @TableField("lock_record_id")
    private Long lockRecordId;

    /** 占用或锁定的团队编号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 占用或锁定的团队名称快照。 */
    @TableField("team_name")
    private String teamName;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getStayDate() { return stayDate; }
    public void setStayDate(LocalDate stayDate) { this.stayDate = stayDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLockRecordId() { return lockRecordId; }
    public void setLockRecordId(Long lockRecordId) { this.lockRecordId = lockRecordId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
}
