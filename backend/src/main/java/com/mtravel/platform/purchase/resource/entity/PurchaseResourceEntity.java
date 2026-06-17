package com.mtravel.platform.purchase.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 采购资源总览实体，对应 purchase_resources 表。
 *
 * <p>资源总览只维护景区、酒店、餐厅、购物等资源本身的主档信息。资源由哪些供应商供给、
 * 采购价多少、优先级如何，统一放在采购关系表中维护。</p>
 */
@TableName("purchase_resources")
public class PurchaseResourceEntity extends TenantSoftDeleteEntity {

    /** 资源类型：scenic、hotel、restaurant、shopping。 */
    @TableField("resource_type")
    private String resourceType;

    /** 资源名称，例如景区、酒店、餐厅或购物店名称。 */
    @TableField("resource_name")
    private String resourceName;

    /** 资源所在省份。 */
    @TableField("province")
    private String province;

    /** 资源所在城市。 */
    @TableField("city")
    private String city;

    /** 资源所在区县。 */
    @TableField("district")
    private String district;

    /** 联系电话。 */
    @TableField("phone")
    private String phone;

    /** 传真号码。 */
    @TableField("fax")
    private String fax;

    /** 详细地址。 */
    @TableField("address")
    private String address;

    /** 温馨提示，用于记录预约、接待或注意事项。 */
    @TableField("warm_tip")
    private String warmTip;

    /** 资源简介。 */
    @TableField("introduction")
    private String introduction;

    /** 资源状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWarmTip() {
        return warmTip;
    }

    public void setWarmTip(String warmTip) {
        this.warmTip = warmTip;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
