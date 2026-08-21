package com.mtravel.platform.purchase.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 采购资源总览实体，对应 purchase_resources 表。
 *
 * <p>资源总览只维护景区、酒店、餐厅、购物、用车、大交通、地接等资源本身的主档信息。资源由哪些供应商供给、
 * 采购价多少、优先级如何，统一放在采购关系表中维护。</p>
 */
@TableName("purchase_resources")
public class PurchaseResourceEntity extends TenantSoftDeleteEntity {

    /** 资源类型：scenic、hotel、restaurant、shopping、vehicle、traffic、ground_agent、other。 */
    @TableField("resource_type")
    private String resourceType;

    /** 默认采购属性：required需要采购，not_required无需采购。 */
    @TableField("procurement_mode")
    private String procurementMode;

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

    /** 资源联系人，独立于供应商联系人。 */
    @TableField("contact_name")
    private String contactName;

    /** 传真号码。 */
    @TableField("fax")
    private String fax;

    /** 详细地址。 */
    @TableField("address")
    private String address;

    /** 酒店或餐厅星级/接待标准，值来自企业产品字典 reception_standard。 */
    @TableField(value = "star_level", updateStrategy = FieldStrategy.ALWAYS)
    private String starLevel;

    /** 类型标签，餐厅可保存菜系，购物可保存商品类别，其它资源可保存业务分类。 */
    @TableField(value = "category_tags", updateStrategy = FieldStrategy.ALWAYS)
    private String categoryTags;

    /** 国家 A 级：unrated、1a、2a、3a、4a、5a，仅景区使用。 */
    @TableField(value = "scenic_level", updateStrategy = FieldStrategy.ALWAYS)
    private String scenicLevel;

    /** 高德 GCJ-02 经度，维护坐标后可进入产品地图。 */
    @TableField(value = "longitude", updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal longitude;

    /** 高德 GCJ-02 纬度，维护坐标后可进入产品地图。 */
    @TableField(value = "latitude", updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal latitude;

    /** 营业状态：unmaintained、open、suspended、closed。 */
    @TableField(value = "business_status", updateStrategy = FieldStrategy.ALWAYS)
    private String businessStatus;

    /** 每日开始营业时间。 */
    @TableField(value = "opening_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime openingTime;

    /** 每日结束营业时间。 */
    @TableField(value = "closing_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime closingTime;

    /** 踩点状态：unmaintained、not_visited、visited。 */
    @TableField(value = "site_visit_status", updateStrategy = FieldStrategy.ALWAYS)
    private String siteVisitStatus;

    /** 最近一次完成踩点的日期。 */
    @TableField(value = "last_site_visit_date", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate lastSiteVisitDate;

    /** 踩点情况备注。 */
    @TableField(value = "site_visit_note", updateStrategy = FieldStrategy.ALWAYS)
    private String siteVisitNote;

    /** 最大接待人数或容量。 */
    @TableField(value = "capacity", updateStrategy = FieldStrategy.ALWAYS)
    private Integer capacity;

    /** 餐厅餐桌数量。 */
    @TableField(value = "table_count", updateStrategy = FieldStrategy.ALWAYS)
    private Integer tableCount;

    /** 团餐标准或资源规格说明。 */
    @TableField(value = "meal_standard", updateStrategy = FieldStrategy.ALWAYS)
    private String mealStandard;

    /** 车辆类型，例如商务车、中巴、大巴。 */
    @TableField(value = "vehicle_type", updateStrategy = FieldStrategy.ALWAYS)
    private String vehicleType;

    /** 车辆座位数。 */
    @TableField(value = "seat_count", updateStrategy = FieldStrategy.ALWAYS)
    private Integer seatCount;

    /** 用车计费模式：daily、trip、distance_time。 */
    @TableField(value = "billing_mode", updateStrategy = FieldStrategy.ALWAYS)
    private String billingMode;

    /** 地接服务地区或大交通服务范围。 */
    @TableField(value = "service_area", updateStrategy = FieldStrategy.ALWAYS)
    private String serviceArea;

    /** 地接参考天数。 */
    @TableField(value = "reference_days", updateStrategy = FieldStrategy.ALWAYS)
    private Integer referenceDays;

    /** 地接或服务类资源包含内容。 */
    @TableField(value = "included_items", updateStrategy = FieldStrategy.ALWAYS)
    private String includedItems;

    /** 地接或服务类资源不包含内容。 */
    @TableField(value = "excluded_items", updateStrategy = FieldStrategy.ALWAYS)
    private String excludedItems;

    /** 其它资源的默认计价单位。 */
    @TableField(value = "resource_unit", updateStrategy = FieldStrategy.ALWAYS)
    private String resourceUnit;

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

    public String getProcurementMode() {
        return procurementMode;
    }

    public void setProcurementMode(String procurementMode) {
        this.procurementMode = procurementMode;
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

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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

    public String getStarLevel() {
        return starLevel;
    }

    public void setStarLevel(String starLevel) {
        this.starLevel = starLevel;
    }

    public String getCategoryTags() {
        return categoryTags;
    }

    public void setCategoryTags(String categoryTags) {
        this.categoryTags = categoryTags;
    }

    public String getScenicLevel() {
        return scenicLevel;
    }

    public void setScenicLevel(String scenicLevel) {
        this.scenicLevel = scenicLevel;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public String getSiteVisitStatus() {
        return siteVisitStatus;
    }

    public void setSiteVisitStatus(String siteVisitStatus) {
        this.siteVisitStatus = siteVisitStatus;
    }

    public LocalDate getLastSiteVisitDate() {
        return lastSiteVisitDate;
    }

    public void setLastSiteVisitDate(LocalDate lastSiteVisitDate) {
        this.lastSiteVisitDate = lastSiteVisitDate;
    }

    public String getSiteVisitNote() {
        return siteVisitNote;
    }

    public void setSiteVisitNote(String siteVisitNote) {
        this.siteVisitNote = siteVisitNote;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getTableCount() {
        return tableCount;
    }

    public void setTableCount(Integer tableCount) {
        this.tableCount = tableCount;
    }

    public String getMealStandard() {
        return mealStandard;
    }

    public void setMealStandard(String mealStandard) {
        this.mealStandard = mealStandard;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public String getBillingMode() {
        return billingMode;
    }

    public void setBillingMode(String billingMode) {
        this.billingMode = billingMode;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public Integer getReferenceDays() {
        return referenceDays;
    }

    public void setReferenceDays(Integer referenceDays) {
        this.referenceDays = referenceDays;
    }

    public String getIncludedItems() {
        return includedItems;
    }

    public void setIncludedItems(String includedItems) {
        this.includedItems = includedItems;
    }

    public String getExcludedItems() {
        return excludedItems;
    }

    public void setExcludedItems(String excludedItems) {
        this.excludedItems = excludedItems;
    }

    public String getResourceUnit() {
        return resourceUnit;
    }

    public void setResourceUnit(String resourceUnit) {
        this.resourceUnit = resourceUnit;
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
