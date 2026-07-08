package com.mtravel.platform.finance.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 购物参考阶梯默认规则实体。
 *
 * <p>该表维护公司层面的购物综合人均消费门槛和佣金比例。团队特殊协议通过团队覆盖表保存，
 * 不直接改写默认规则。</p>
 */
@TableName("finance_shopping_commission_rules")
public class FinanceShoppingCommissionRuleEntity extends TenantSoftDeleteEntity {

    /** 规则名称。 */
    @TableField("rule_name")
    private String ruleName;

    /** 人均消费门槛金额。 */
    @TableField("threshold_per_capita_amount")
    private BigDecimal thresholdPerCapitaAmount;

    /** 基础导游佣金比例，按百分数保存。 */
    @TableField("base_commission_rate")
    private BigDecimal baseCommissionRate;

    /** 达标后目标导游佣金比例，按百分数保存。 */
    @TableField("target_commission_rate")
    private BigDecimal targetCommissionRate;

    /** 阶梯补差方式。full_amount_diff 表示达标后按全额补差。 */
    @TableField("ladder_calc_mode")
    private String ladderCalcMode;

    /** 生效开始日期。 */
    @TableField("effective_start_date")
    private LocalDate effectiveStartDate;

    /** 生效结束日期。 */
    @TableField("effective_end_date")
    private LocalDate effectiveEndDate;

    /** 规则状态。active启用，disabled停用。 */
    @TableField("status")
    private String status;

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public BigDecimal getThresholdPerCapitaAmount() { return thresholdPerCapitaAmount; }
    public void setThresholdPerCapitaAmount(BigDecimal thresholdPerCapitaAmount) { this.thresholdPerCapitaAmount = thresholdPerCapitaAmount; }
    public BigDecimal getBaseCommissionRate() { return baseCommissionRate; }
    public void setBaseCommissionRate(BigDecimal baseCommissionRate) { this.baseCommissionRate = baseCommissionRate; }
    public BigDecimal getTargetCommissionRate() { return targetCommissionRate; }
    public void setTargetCommissionRate(BigDecimal targetCommissionRate) { this.targetCommissionRate = targetCommissionRate; }
    public String getLadderCalcMode() { return ladderCalcMode; }
    public void setLadderCalcMode(String ladderCalcMode) { this.ladderCalcMode = ladderCalcMode; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public void setEffectiveStartDate(LocalDate effectiveStartDate) { this.effectiveStartDate = effectiveStartDate; }
    public LocalDate getEffectiveEndDate() { return effectiveEndDate; }
    public void setEffectiveEndDate(LocalDate effectiveEndDate) { this.effectiveEndDate = effectiveEndDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
