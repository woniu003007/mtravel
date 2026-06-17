package com.mtravel.platform.dispatch.roomstatus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 自控房间明细实体。
 *
 * <p>一条自控房源批次下可以维护多个具体房间。后续排房和锁房必须落到具体房号，
 * 不能只按房型数量扣减，否则无法判断哪间房被哪个团队占用。</p>
 */
@TableName("controlled_room_units")
public class ControlledRoomUnitEntity extends TenantSoftDeleteEntity {

    /** 所属自控房源批次 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 楼栋名称或编号。 */
    @TableField("building_name")
    private String buildingName;

    /** 楼层。 */
    @TableField("floor_no")
    private String floorNo;

    /** 房号。 */
    @TableField("room_no")
    private String roomNo;

    /** 房型名称，默认与自控房源批次房型一致，也允许局部细分。 */
    @TableField("room_type")
    private String roomType;

    /** 床型，例如双床、大床、亲子。 */
    @TableField("bed_type")
    private String bedType;

    /** 可住人数。 */
    @TableField("capacity")
    private Integer capacity;

    /** 房间状态：active 启用，disabled 停用，maintenance 维修。 */
    @TableField("status")
    private String status;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getFloorNo() { return floorNo; }
    public void setFloorNo(String floorNo) { this.floorNo = floorNo; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
