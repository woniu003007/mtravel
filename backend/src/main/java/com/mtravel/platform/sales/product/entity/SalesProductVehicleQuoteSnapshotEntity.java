package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售产品用车报价测算快照实体。
 *
 * <p>一条产品团队安排用车项目最多保留一条未删除快照。保存快照是为了让产品模板中的参考价
 * 不受后续车型报价规则调整影响。</p>
 */
@TableName("sales_product_vehicle_quote_snapshots")
public class SalesProductVehicleQuoteSnapshotEntity extends TenantSoftDeleteEntity {

    @TableField("product_id") private Long productId;
    @TableField("arrangement_item_id") private Long arrangementItemId;
    @TableField("schedule_start_day") private String scheduleStartDay;
    @TableField("schedule_end_day") private String scheduleEndDay;
    @TableField("start_day_no") private Integer startDayNo;
    @TableField("end_day_no") private Integer endDayNo;
    @TableField("synced_distance_meters") private Integer syncedDistanceMeters;
    @TableField("synced_duration_seconds") private Integer syncedDurationSeconds;
    @TableField("route_summary") private String routeSummary;
    @TableField("quote_rule_id") private Long quoteRuleId;
    @TableField("rule_vehicle_type") private String ruleVehicleType;
    @TableField("rule_province") private String ruleProvince;
    @TableField("rule_city") private String ruleCity;
    @TableField("rule_district") private String ruleDistrict;
    @TableField("rule_base_price") private BigDecimal ruleBasePrice;
    @TableField("rule_base_kilometers") private BigDecimal ruleBaseKilometers;
    @TableField("rule_extra_kilometer_price") private BigDecimal ruleExtraKilometerPrice;
    @TableField("rule_minimum_price") private BigDecimal ruleMinimumPrice;
    @TableField("rule_float_rate") private BigDecimal ruleFloatRate;
    @TableField("calculated_amount") private BigDecimal calculatedAmount;
    @TableField("confirmed_amount") private BigDecimal confirmedAmount;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getArrangementItemId() { return arrangementItemId; }
    public void setArrangementItemId(Long arrangementItemId) { this.arrangementItemId = arrangementItemId; }
    public String getScheduleStartDay() { return scheduleStartDay; }
    public void setScheduleStartDay(String scheduleStartDay) { this.scheduleStartDay = scheduleStartDay; }
    public String getScheduleEndDay() { return scheduleEndDay; }
    public void setScheduleEndDay(String scheduleEndDay) { this.scheduleEndDay = scheduleEndDay; }
    public Integer getStartDayNo() { return startDayNo; }
    public void setStartDayNo(Integer startDayNo) { this.startDayNo = startDayNo; }
    public Integer getEndDayNo() { return endDayNo; }
    public void setEndDayNo(Integer endDayNo) { this.endDayNo = endDayNo; }
    public Integer getSyncedDistanceMeters() { return syncedDistanceMeters; }
    public void setSyncedDistanceMeters(Integer syncedDistanceMeters) { this.syncedDistanceMeters = syncedDistanceMeters; }
    public Integer getSyncedDurationSeconds() { return syncedDurationSeconds; }
    public void setSyncedDurationSeconds(Integer syncedDurationSeconds) { this.syncedDurationSeconds = syncedDurationSeconds; }
    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }
    public Long getQuoteRuleId() { return quoteRuleId; }
    public void setQuoteRuleId(Long quoteRuleId) { this.quoteRuleId = quoteRuleId; }
    public String getRuleVehicleType() { return ruleVehicleType; }
    public void setRuleVehicleType(String ruleVehicleType) { this.ruleVehicleType = ruleVehicleType; }
    public String getRuleProvince() { return ruleProvince; }
    public void setRuleProvince(String ruleProvince) { this.ruleProvince = ruleProvince; }
    public String getRuleCity() { return ruleCity; }
    public void setRuleCity(String ruleCity) { this.ruleCity = ruleCity; }
    public String getRuleDistrict() { return ruleDistrict; }
    public void setRuleDistrict(String ruleDistrict) { this.ruleDistrict = ruleDistrict; }
    public BigDecimal getRuleBasePrice() { return ruleBasePrice; }
    public void setRuleBasePrice(BigDecimal ruleBasePrice) { this.ruleBasePrice = ruleBasePrice; }
    public BigDecimal getRuleBaseKilometers() { return ruleBaseKilometers; }
    public void setRuleBaseKilometers(BigDecimal ruleBaseKilometers) { this.ruleBaseKilometers = ruleBaseKilometers; }
    public BigDecimal getRuleExtraKilometerPrice() { return ruleExtraKilometerPrice; }
    public void setRuleExtraKilometerPrice(BigDecimal ruleExtraKilometerPrice) { this.ruleExtraKilometerPrice = ruleExtraKilometerPrice; }
    public BigDecimal getRuleMinimumPrice() { return ruleMinimumPrice; }
    public void setRuleMinimumPrice(BigDecimal ruleMinimumPrice) { this.ruleMinimumPrice = ruleMinimumPrice; }
    public BigDecimal getRuleFloatRate() { return ruleFloatRate; }
    public void setRuleFloatRate(BigDecimal ruleFloatRate) { this.ruleFloatRate = ruleFloatRate; }
    public BigDecimal getCalculatedAmount() { return calculatedAmount; }
    public void setCalculatedAmount(BigDecimal calculatedAmount) { this.calculatedAmount = calculatedAmount; }
    public BigDecimal getConfirmedAmount() { return confirmedAmount; }
    public void setConfirmedAmount(BigDecimal confirmedAmount) { this.confirmedAmount = confirmedAmount; }
}
