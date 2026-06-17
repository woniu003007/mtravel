package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售产品模板主实体，对应 sales_products 表。
 *
 * <p>产品模板保存线路的基础资料，后续团期、团队、订单从这里继承名称、天数、标准和默认规则。</p>
 */
@TableName("sales_products")
public class SalesProductEntity extends TenantSoftDeleteEntity {

    /** 产品名称，也就是线路名称。 */
    @TableField("product_name")
    private String productName;

    /** 业务类型，例如疗休养、定制团、地接团。 */
    @TableField("business_type")
    private String businessType;

    /** 国内国际标记。domestic 国内，international 国际。 */
    @TableField("domestic_international")
    private String domesticInternational;

    /** 接团省份。 */
    @TableField("province")
    private String province;

    /** 接团城市。 */
    @TableField("city")
    private String city;

    /** 接团区县。 */
    @TableField("district")
    private String district;

    /** 出团类型。daily 每天发，weekly 每周发，irregular 不定期。 */
    @TableField("trip_type")
    private String tripType;

    /** 接待标准。 */
    @TableField("reception_standard")
    private String receptionStandard;

    /** 产品主题。 */
    @TableField("product_theme")
    private String productTheme;

    /** 旅游天数。 */
    @TableField("travel_days")
    private Integer travelDays;

    /** 出团前多少天截止收客。 */
    @TableField("close_days_before")
    private Integer closeDaysBefore;

    /** 单人房差金额。 */
    @TableField("single_room_difference")
    private BigDecimal singleRoomDifference;

    /** 预控人数，用于团期默认容量参考。 */
    @TableField("planned_capacity")
    private Integer plannedCapacity;

    /** 产品状态。active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getDomesticInternational() {
        return domesticInternational;
    }

    public void setDomesticInternational(String domesticInternational) {
        this.domesticInternational = domesticInternational;
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

    public String getTripType() {
        return tripType;
    }

    public void setTripType(String tripType) {
        this.tripType = tripType;
    }

    public String getReceptionStandard() {
        return receptionStandard;
    }

    public void setReceptionStandard(String receptionStandard) {
        this.receptionStandard = receptionStandard;
    }

    public String getProductTheme() {
        return productTheme;
    }

    public void setProductTheme(String productTheme) {
        this.productTheme = productTheme;
    }

    public Integer getTravelDays() {
        return travelDays;
    }

    public void setTravelDays(Integer travelDays) {
        this.travelDays = travelDays;
    }

    public Integer getCloseDaysBefore() {
        return closeDaysBefore;
    }

    public void setCloseDaysBefore(Integer closeDaysBefore) {
        this.closeDaysBefore = closeDaysBefore;
    }

    public BigDecimal getSingleRoomDifference() {
        return singleRoomDifference;
    }

    public void setSingleRoomDifference(BigDecimal singleRoomDifference) {
        this.singleRoomDifference = singleRoomDifference;
    }

    public Integer getPlannedCapacity() {
        return plannedCapacity;
    }

    public void setPlannedCapacity(Integer plannedCapacity) {
        this.plannedCapacity = plannedCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
