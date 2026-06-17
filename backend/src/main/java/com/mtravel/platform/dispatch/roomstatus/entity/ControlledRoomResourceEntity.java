package com.mtravel.platform.dispatch.roomstatus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 自控房源批次实体。
 *
 * <p>自控房源表示企业通过买断、包房或长期协议掌握使用权的酒店档案。酒店层只保存酒店、
 * 所在地、星钻标准、有效期和联系人；具体标间、大床房、三人间等放到房型表维护。</p>
 */
@TableName("controlled_room_resources")
public class ControlledRoomResourceEntity extends TenantSoftDeleteEntity {

    /** 酒店名称。 */
    @TableField("hotel_name")
    private String hotelName;

    /** 酒店所在省份。 */
    @TableField("province")
    private String province;

    /** 酒店所在城市。 */
    @TableField("city")
    private String city;

    /** 酒店所在区县。 */
    @TableField("district")
    private String district;

    /** 酒店所在区域或商圈。 */
    @TableField("area")
    private String area;

    /** 酒店详细地址。 */
    @TableField("address")
    private String address;

    /** 星级、钻级或内部住宿标准，后续排房按该字段匹配团队住宿标准。 */
    @TableField("star_standard")
    private String starStandard;

    /** 历史兼容房型字段。新业务房型放在 controlled_room_types 表。 */
    @TableField("room_type")
    private String roomType;

    /** 房源来源或酒店方名称，普通文本，不关联采购供应商。 */
    @TableField("source_name")
    private String sourceName;

    /** 历史兼容采购价字段。新业务采购价放在房型表。 */
    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    /** 历史兼容协议价字段。新业务协议价放在房型表。 */
    @TableField("agreement_price")
    private BigDecimal agreementPrice;

    /** 历史兼容价格单位字段。新业务价格单位放在房型表。 */
    @TableField("price_unit")
    private String priceUnit;

    /** 房源使用权有效期开始日期。 */
    @TableField("valid_from")
    private LocalDate validFrom;

    /** 房源使用权有效期结束日期。 */
    @TableField("valid_to")
    private LocalDate validTo;

    /** 联系人姓名。 */
    @TableField("contact_name")
    private String contactName;

    /** 联系电话。 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 房源状态：active 启用，disabled 停用，expired 到期。 */
    @TableField("status")
    private String status;

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getStarStandard() { return starStandard; }
    public void setStarStandard(String starStandard) { this.starStandard = starStandard; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getAgreementPrice() { return agreementPrice; }
    public void setAgreementPrice(BigDecimal agreementPrice) { this.agreementPrice = agreementPrice; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
