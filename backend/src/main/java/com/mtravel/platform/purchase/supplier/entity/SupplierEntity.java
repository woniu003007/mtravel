package com.mtravel.platform.purchase.supplier.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 供应商实体，对应 suppliers 表。
 *
 * <p>供应商是采购侧的基础档案，酒店、景区、采购关系、采购合同等模块都会引用它。</p>
 */
@TableName("suppliers")
public class SupplierEntity extends TenantSoftDeleteEntity {
    @TableField("supplier_code") private String supplierCode;
    @TableField("supplier_name") private String supplierName;
    @TableField("supplier_category") private String supplierCategory;
    /** 关联采购商客户单位 ID，用于财务应收应付冲抵。为空表示不参与冲抵关系。 */
    @TableField("buyer_id") private Long buyerId;
    @TableField("province") private String province;
    @TableField("city") private String city;
    @TableField("district") private String district;
    @TableField("settlement_method") private String settlementMethod;
    /** 供应商基础信息，资源页快捷新增时用于记录供应商简介、接待能力等文本。 */
    @TableField("basic_info") private String basicInfo;
    @TableField("contact_name") private String contactName;
    @TableField("contact_phone") private String contactPhone;
    @TableField("fax_number") private String faxNumber;
    @TableField("office_address") private String officeAddress;
    @TableField("agreement_name") private String agreementName;
    @TableField("rating") private Integer rating;
    @TableField("status") private String status;
    public String getSupplierCode(){return supplierCode;} public void setSupplierCode(String supplierCode){this.supplierCode=supplierCode;}
    public String getSupplierName(){return supplierName;} public void setSupplierName(String supplierName){this.supplierName=supplierName;}
    public String getSupplierCategory(){return supplierCategory;} public void setSupplierCategory(String supplierCategory){this.supplierCategory=supplierCategory;}
    public Long getBuyerId(){return buyerId;} public void setBuyerId(Long buyerId){this.buyerId=buyerId;}
    public String getProvince(){return province;} public void setProvince(String province){this.province=province;}
    public String getCity(){return city;} public void setCity(String city){this.city=city;}
    public String getDistrict(){return district;} public void setDistrict(String district){this.district=district;}
    public String getSettlementMethod(){return settlementMethod;} public void setSettlementMethod(String settlementMethod){this.settlementMethod=settlementMethod;}
    public String getBasicInfo(){return basicInfo;} public void setBasicInfo(String basicInfo){this.basicInfo=basicInfo;}
    public String getContactName(){return contactName;} public void setContactName(String contactName){this.contactName=contactName;}
    public String getContactPhone(){return contactPhone;} public void setContactPhone(String contactPhone){this.contactPhone=contactPhone;}
    public String getFaxNumber(){return faxNumber;} public void setFaxNumber(String faxNumber){this.faxNumber=faxNumber;}
    public String getOfficeAddress(){return officeAddress;} public void setOfficeAddress(String officeAddress){this.officeAddress=officeAddress;}
    public String getAgreementName(){return agreementName;} public void setAgreementName(String agreementName){this.agreementName=agreementName;}
    public Integer getRating(){return rating;} public void setRating(Integer rating){this.rating=rating;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
