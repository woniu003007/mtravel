package com.mtravel.platform.purchase.scenic.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 景区资源实体，对应 scenic_resources 表。 */
@TableName("scenic_resources")
public class ScenicResourceEntity extends TenantSoftDeleteEntity {
    @TableField("scenic_name") private String scenicName;
    @TableField("city") private String city;
    @TableField("area") private String area;
    @TableField("address") private String address;
    @TableField("ticket_type") private String ticketType;
    @TableField("supplier_id") private Long supplierId;
    @TableField("purchase_price") private BigDecimal purchasePrice;
    @TableField("agreement_price") private BigDecimal agreementPrice;
    @TableField("price_unit") private String priceUnit;
    @TableField("valid_from") private LocalDate validFrom;
    @TableField("valid_to") private LocalDate validTo;
    @TableField("free_ticket_rule") private String freeTicketRule;
    @TableField("half_ticket_rule") private String halfTicketRule;
    @TableField("contact_name") private String contactName;
    @TableField("contact_phone") private String contactPhone;
    @TableField("status") private String status;
    public String getScenicName(){return scenicName;} public void setScenicName(String scenicName){this.scenicName=scenicName;}
    public String getCity(){return city;} public void setCity(String city){this.city=city;}
    public String getArea(){return area;} public void setArea(String area){this.area=area;}
    public String getAddress(){return address;} public void setAddress(String address){this.address=address;}
    public String getTicketType(){return ticketType;} public void setTicketType(String ticketType){this.ticketType=ticketType;}
    public Long getSupplierId(){return supplierId;} public void setSupplierId(Long supplierId){this.supplierId=supplierId;}
    public BigDecimal getPurchasePrice(){return purchasePrice;} public void setPurchasePrice(BigDecimal purchasePrice){this.purchasePrice=purchasePrice;}
    public BigDecimal getAgreementPrice(){return agreementPrice;} public void setAgreementPrice(BigDecimal agreementPrice){this.agreementPrice=agreementPrice;}
    public String getPriceUnit(){return priceUnit;} public void setPriceUnit(String priceUnit){this.priceUnit=priceUnit;}
    public LocalDate getValidFrom(){return validFrom;} public void setValidFrom(LocalDate validFrom){this.validFrom=validFrom;}
    public LocalDate getValidTo(){return validTo;} public void setValidTo(LocalDate validTo){this.validTo=validTo;}
    public String getFreeTicketRule(){return freeTicketRule;} public void setFreeTicketRule(String freeTicketRule){this.freeTicketRule=freeTicketRule;}
    public String getHalfTicketRule(){return halfTicketRule;} public void setHalfTicketRule(String halfTicketRule){this.halfTicketRule=halfTicketRule;}
    public String getContactName(){return contactName;} public void setContactName(String contactName){this.contactName=contactName;}
    public String getContactPhone(){return contactPhone;} public void setContactPhone(String contactPhone){this.contactPhone=contactPhone;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
