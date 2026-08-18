package com.mtravel.platform.configuration.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 导游销售报价规则实体。
 */
@TableName("sales_quote_guide_rules")
public class SalesQuoteGuideRuleEntity extends TenantSoftDeleteEntity {

    /** 导游等级 ID。 */
    @TableField("guide_level_id")
    private Long guideLevelId;

    /** 导游等级名称快照。 */
    @TableField("guide_level_name")
    private String guideLevelName;

    /** 服务语种。 */
    @TableField("language")
    private String language;

    /** 基础导服费，按天计费。 */
    @TableField("base_daily_fee")
    private BigDecimal baseDailyFee;

    /** 外语服务按天加价。 */
    @TableField("foreign_language_daily_markup")
    private BigDecimal foreignLanguageDailyMarkup;

    /** 超时费，按小时计费。 */
    @TableField("overtime_hourly_fee")
    private BigDecimal overtimeHourlyFee;

    /** 规则状态。 */
    @TableField("status")
    private String status;

    public Long getGuideLevelId() { return guideLevelId; }
    public void setGuideLevelId(Long guideLevelId) { this.guideLevelId = guideLevelId; }
    public String getGuideLevelName() { return guideLevelName; }
    public void setGuideLevelName(String guideLevelName) { this.guideLevelName = guideLevelName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public BigDecimal getBaseDailyFee() { return baseDailyFee; }
    public void setBaseDailyFee(BigDecimal baseDailyFee) { this.baseDailyFee = baseDailyFee; }
    public BigDecimal getForeignLanguageDailyMarkup() { return foreignLanguageDailyMarkup; }
    public void setForeignLanguageDailyMarkup(BigDecimal foreignLanguageDailyMarkup) { this.foreignLanguageDailyMarkup = foreignLanguageDailyMarkup; }
    public BigDecimal getOvertimeHourlyFee() { return overtimeHourlyFee; }
    public void setOvertimeHourlyFee(BigDecimal overtimeHourlyFee) { this.overtimeHourlyFee = overtimeHourlyFee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
