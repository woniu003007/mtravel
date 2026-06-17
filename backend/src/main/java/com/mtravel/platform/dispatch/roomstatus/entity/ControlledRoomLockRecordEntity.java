package com.mtravel.platform.dispatch.roomstatus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 自控房源锁房流水实体。
 *
 * <p>锁房是排房前的预占操作，不能只改每日房态而不留流水。流水用于后续查看占用来源、
 * 释放锁房、转为实际占用，以及排查同一天重复占用问题。</p>
 */
@TableName("controlled_room_lock_records")
public class ControlledRoomLockRecordEntity extends TenantSoftDeleteEntity {

    /** 自控房源批次 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 来源类型：self_owned 自营房源，purchased_resource 资源采购房源。 */
    @TableField("source_type")
    private String sourceType;

    /** 来源 ID。自营房源为自营酒店档案 ID，资源采购房源为采购关系 ID。 */
    @TableField("source_id")
    private Long sourceId;

    /** 自营房型 ID。采购房源或历史房号锁房记录可为空。 */
    @TableField("room_type_id")
    private Long roomTypeId;

    /** 房型名称快照。 */
    @TableField("room_type")
    private String roomType;

    /** 锁房数量。按房型数量锁房时使用；历史按房号锁房记录默认为 1。 */
    @TableField("quantity")
    private Integer quantity;

    /** 具体房间 ID。按房型数量锁房时为空，仅自营房源后续细排房号时使用。 */
    @TableField("room_id")
    private Long roomId;

    /** 入住日期，含当天。 */
    @TableField("check_in_date")
    private LocalDate checkInDate;

    /** 退房日期，不含当天。 */
    @TableField("check_out_date")
    private LocalDate checkOutDate;

    /** 团队编号。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队名称。 */
    @TableField("team_name")
    private String teamName;

    /** 团队住宿标准，用于和房源星钻标准做差异提醒。 */
    @TableField("required_standard")
    private String requiredStandard;

    /** 锁房状态：locked 已锁定，occupied 已转占用，released 已释放。 */
    @TableField("status")
    private String status;

    /** 释放时间。未释放时为空。 */
    @TableField("released_at")
    private OffsetDateTime releasedAt;

    /** 释放人账号或名称。 */
    @TableField("released_by")
    private String releasedBy;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Long roomTypeId) { this.roomTypeId = roomTypeId; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getRequiredStandard() { return requiredStandard; }
    public void setRequiredStandard(String requiredStandard) { this.requiredStandard = requiredStandard; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(OffsetDateTime releasedAt) { this.releasedAt = releasedAt; }
    public String getReleasedBy() { return releasedBy; }
    public void setReleasedBy(String releasedBy) { this.releasedBy = releasedBy; }
}
