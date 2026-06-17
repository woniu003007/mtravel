package com.mtravel.platform.purchase.hotel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 酒店资源实体，对应 hotel_resources 表。 */
@TableName("hotel_resources")
public class HotelResourceEntity extends TenantSoftDeleteEntity {
    @TableField("hotel_name") private String hotelName;
    @TableField("city") private String city;
    @TableField("area") private String area;
    @TableField("address") private String address;
    @TableField("star_standard") private String starStandard;
    @TableField("room_type") private String roomType;
    @TableField("supplier_id") private Long supplierId;
    @TableField("purchase_price") private BigDecimal purchasePrice;
    @TableField("agreement_price") private BigDecimal agreementPrice;
    @TableField("price_unit") private String priceUnit;
    @TableField("valid_from") private LocalDate validFrom;
    @TableField("valid_to") private LocalDate validTo;
    @TableField("contact_name") private String contactName;
    @TableField("contact_phone") private String contactPhone;
    @TableField("status") private String status;
    public String getHotelName(){return hotelName;} public void setHotelName(String hotelName){this.hotelName=hotelName;}
    public String getCity(){return city;} public void setCity(String city){this.city=city;}
    public String getArea(){return area;} public void setArea(String area){this.area=area;}
    public String getAddress(){return address;} public void setAddress(String address){this.address=address;}
    public String getStarStandard(){return starStandard;} public void setStarStandard(String starStandard){this.starStandard=starStandard;}
    public String getRoomType(){return roomType;} public void setRoomType(String roomType){this.roomType=roomType;}
    public Long getSupplierId(){return supplierId;} public void setSupplierId(Long supplierId){this.supplierId=supplierId;}
    public BigDecimal getPurchasePrice(){return purchasePrice;} public void setPurchasePrice(BigDecimal purchasePrice){this.purchasePrice=purchasePrice;}
    public BigDecimal getAgreementPrice(){return agreementPrice;} public void setAgreementPrice(BigDecimal agreementPrice){this.agreementPrice=agreementPrice;}
    public String getPriceUnit(){return priceUnit;} public void setPriceUnit(String priceUnit){this.priceUnit=priceUnit;}
    public LocalDate getValidFrom(){return validFrom;} public void setValidFrom(LocalDate validFrom){this.validFrom=validFrom;}
    public LocalDate getValidTo(){return validTo;} public void setValidTo(LocalDate validTo){this.validTo=validTo;}
    public String getContactName(){return contactName;} public void setContactName(String contactName){this.contactName=contactName;}
    public String getContactPhone(){return contactPhone;} public void setContactPhone(String contactPhone){this.contactPhone=contactPhone;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
