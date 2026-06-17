package com.mtravel.platform.dispatch.roomstatus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;

/**
 * 每日房态库存实体。
 *
 * <p>库存按来源、酒店/采购关系、房型和日期聚合，直接表达老系统中的“总量/余量”，
 * 并额外拆出已锁和已占，方便计调排房。</p>
 */
@TableName("room_inventories")
public class RoomInventoryEntity extends TenantSoftDeleteEntity {

    /** 来源类型：self_owned 自营房源，purchased_resource 资源采购房源。 */
    @TableField("source_type")
    private String sourceType;

    /** 来源 ID。自营房源为 controlled_room_resources.id，采购房源为 purchase_relations.id。 */
    @TableField("source_id")
    private Long sourceId;

    /** 自营房型 ID。采购房源为空。 */
    @TableField("room_type_id")
    private Long roomTypeId;

    /** 酒店名称快照。 */
    @TableField("hotel_name")
    private String hotelName;

    /** 供应商名称快照，自营房源可为空。 */
    @TableField("supplier_name")
    private String supplierName;

    /** 房型名称快照。 */
    @TableField("room_type")
    private String roomType;

    /** 住宿日期。 */
    @TableField("stay_date")
    private LocalDate stayDate;

    /** 总库存间数。 */
    @TableField("total_quantity")
    private Integer totalQuantity;

    /** 已锁定间数。 */
    @TableField("locked_quantity")
    private Integer lockedQuantity;

    /** 已占用间数。 */
    @TableField("occupied_quantity")
    private Integer occupiedQuantity;

    /** 剩余可用间数。 */
    @TableField("remaining_quantity")
    private Integer remainingQuantity;

    /** 库存状态：active 有效，stopped 停售。 */
    @TableField("status")
    private String status;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Long roomTypeId) { this.roomTypeId = roomTypeId; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public LocalDate getStayDate() { return stayDate; }
    public void setStayDate(LocalDate stayDate) { this.stayDate = stayDate; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getLockedQuantity() { return lockedQuantity; }
    public void setLockedQuantity(Integer lockedQuantity) { this.lockedQuantity = lockedQuantity; }
    public Integer getOccupiedQuantity() { return occupiedQuantity; }
    public void setOccupiedQuantity(Integer occupiedQuantity) { this.occupiedQuantity = occupiedQuantity; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
