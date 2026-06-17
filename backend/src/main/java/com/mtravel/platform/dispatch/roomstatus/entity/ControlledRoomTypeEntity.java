package com.mtravel.platform.dispatch.roomstatus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 自营房型实体。
 *
 * <p>同一家自营酒店可以维护多个房型。房型层保存床型、可住人数和价格，
 * 房源档案层只保存酒店本身信息。</p>
 */
@TableName("controlled_room_types")
public class ControlledRoomTypeEntity extends TenantSoftDeleteEntity {

    /** 所属自营房源档案 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 房型名称。 */
    @TableField("room_type")
    private String roomType;

    /** 床型。 */
    @TableField("bed_type")
    private String bedType;

    /** 可住人数。 */
    @TableField("capacity")
    private Integer capacity;

    /** 采购价。 */
    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    /** 协议价。 */
    @TableField("agreement_price")
    private BigDecimal agreementPrice;

    /** 价格单位。 */
    @TableField("price_unit")
    private String priceUnit;

    /** 状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getAgreementPrice() { return agreementPrice; }
    public void setAgreementPrice(BigDecimal agreementPrice) { this.agreementPrice = agreementPrice; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
