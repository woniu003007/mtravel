package com.mtravel.platform.finance.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 团队购物参考阶梯规则覆盖实体。
 *
 * <p>计调可按团队调整门槛和比例。覆盖记录保存当时的规则快照和原因，
 * 用于后续导游结算、财务审核和争议追溯。</p>
 */
@TableName("finance_shopping_team_rule_overrides")
public class FinanceShoppingTeamRuleOverrideEntity extends TenantSoftDeleteEntity {

    /** 团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 人均消费门槛金额。 */
    @TableField("threshold_per_capita_amount")
    private BigDecimal thresholdPerCapitaAmount;

    /** 基础导游佣金比例，按百分数保存。 */
    @TableField("base_commission_rate")
    private BigDecimal baseCommissionRate;

    /** 达标后目标导游佣金比例，按百分数保存。 */
    @TableField("target_commission_rate")
    private BigDecimal targetCommissionRate;

    /** 阶梯补差方式。 */
    @TableField("ladder_calc_mode")
    private String ladderCalcMode;

    /** 覆盖原因。 */
    @TableField("override_reason")
    private String overrideReason;

    /** 覆盖操作人。 */
    @TableField("overridden_by")
    private String overriddenBy;

    /** 覆盖时间。 */
    @TableField("overridden_at")
    private OffsetDateTime overriddenAt;

    /** 状态。active生效，superseded被新规则替代，cancelled作废。 */
    @TableField("status")
    private String status;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public BigDecimal getThresholdPerCapitaAmount() { return thresholdPerCapitaAmount; }
    public void setThresholdPerCapitaAmount(BigDecimal thresholdPerCapitaAmount) { this.thresholdPerCapitaAmount = thresholdPerCapitaAmount; }
    public BigDecimal getBaseCommissionRate() { return baseCommissionRate; }
    public void setBaseCommissionRate(BigDecimal baseCommissionRate) { this.baseCommissionRate = baseCommissionRate; }
    public BigDecimal getTargetCommissionRate() { return targetCommissionRate; }
    public void setTargetCommissionRate(BigDecimal targetCommissionRate) { this.targetCommissionRate = targetCommissionRate; }
    public String getLadderCalcMode() { return ladderCalcMode; }
    public void setLadderCalcMode(String ladderCalcMode) { this.ladderCalcMode = ladderCalcMode; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public String getOverriddenBy() { return overriddenBy; }
    public void setOverriddenBy(String overriddenBy) { this.overriddenBy = overriddenBy; }
    public OffsetDateTime getOverriddenAt() { return overriddenAt; }
    public void setOverriddenAt(OffsetDateTime overriddenAt) { this.overriddenAt = overriddenAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
