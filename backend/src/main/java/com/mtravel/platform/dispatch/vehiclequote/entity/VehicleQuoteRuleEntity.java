package com.mtravel.platform.dispatch.vehiclequote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 座位数报价规则实体，对应 vehicle_quote_rules 表。
 *
 * <p>该规则用于按车辆座位数和路书公里测算用车参考价。它不是正式派车单成本，
 * 正式成本仍以后续团队履约和财务确认为准。</p>
 */
@TableName("vehicle_quote_rules")
public class VehicleQuoteRuleEntity extends TenantSoftDeleteEntity {

    /** 车辆座位数，例如 7座、39座。字段名沿用 vehicle_type，前端展示为“座位数”。 */
    @TableField("vehicle_type")
    private String vehicleType;

    /** 预留省份字段，当前座位数报价规则暂不按地区区分。 */
    @TableField("province")
    private String province;

    /** 预留城市字段，当前座位数报价规则暂不按地区区分。 */
    @TableField("city")
    private String city;

    /** 预留区县字段，当前座位数报价规则暂不按地区区分。 */
    @TableField("district")
    private String district;

    /** 基础价，覆盖基础公里以内的参考费用。 */
    @TableField("base_price")
    private BigDecimal basePrice;

    /** 基础公里数。 */
    @TableField("base_kilometers")
    private BigDecimal baseKilometers;

    /** 超出基础公里后的每公里价格。 */
    @TableField("extra_kilometer_price")
    private BigDecimal extraKilometerPrice;

    /** 最低报价，防止短途测算结果过低。 */
    @TableField("minimum_price")
    private BigDecimal minimumPrice;

    /** 浮动系数，例如旺季或偏远线路可设置 1.10。 */
    @TableField("float_rate")
    private BigDecimal floatRate;

    /** 启停状态。active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public BigDecimal getBaseKilometers() { return baseKilometers; }
    public void setBaseKilometers(BigDecimal baseKilometers) { this.baseKilometers = baseKilometers; }
    public BigDecimal getExtraKilometerPrice() { return extraKilometerPrice; }
    public void setExtraKilometerPrice(BigDecimal extraKilometerPrice) { this.extraKilometerPrice = extraKilometerPrice; }
    public BigDecimal getMinimumPrice() { return minimumPrice; }
    public void setMinimumPrice(BigDecimal minimumPrice) { this.minimumPrice = minimumPrice; }
    public BigDecimal getFloatRate() { return floatRate; }
    public void setFloatRate(BigDecimal floatRate) { this.floatRate = floatRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
