package com.mtravel.platform.finance.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 购物佣金结算快照实体。
 *
 * <p>每次重新计算都会生成一份快照，保存当时的团队人数、规则、反馈汇总和内外账口径金额。
 * 旧快照不覆盖，用状态标记是否仍是当前有效结果。</p>
 */
@TableName("finance_shopping_settlements")
public class FinanceShoppingSettlementEntity extends TenantSoftDeleteEntity {

    /** 团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队类型快照。 */
    @TableField("team_type")
    private String teamType;

    /** 业务类型快照。 */
    @TableField("business_type")
    private String businessType;

    /** 发团日期快照。 */
    @TableField("departure_date")
    private LocalDate departureDate;

    /** 规则来源。default_rule默认规则，team_override团队覆盖。 */
    @TableField("rule_source")
    private String ruleSource;

    /** 团队实收人数。 */
    @TableField("guest_count")
    private Integer guestCount;

    /** 人均消费门槛金额。 */
    @TableField("threshold_per_capita_amount")
    private BigDecimal thresholdPerCapitaAmount;

    /** 基础导游佣金比例。 */
    @TableField("base_commission_rate")
    private BigDecimal baseCommissionRate;

    /** 达标后目标导游佣金比例。 */
    @TableField("target_commission_rate")
    private BigDecimal targetCommissionRate;

    /** 阶梯补差方式。 */
    @TableField("ladder_calc_mode")
    private String ladderCalcMode;

    /** 全团购物消费总额。 */
    @TableField("total_consumption_amount")
    private BigDecimal totalConsumptionAmount;

    /** 团队人均购物消费。 */
    @TableField("per_capita_consumption_amount")
    private BigDecimal perCapitaConsumptionAmount;

    /** 是否达到阶梯门槛。 */
    @TableField("threshold_reached")
    private Boolean thresholdReached;

    /** 基础导游佣金金额。 */
    @TableField("base_guide_commission_amount")
    private BigDecimal baseGuideCommissionAmount;

    /** 参考阶梯补差佣金金额，不自动进入正式成本。 */
    @TableField("ladder_extra_commission_amount")
    private BigDecimal ladderExtraCommissionAmount;

    /** 导游现场佣金和参考补差的展示合计。 */
    @TableField("guide_commission_total_amount")
    private BigDecimal guideCommissionTotalAmount;

    /** 计调确认由公司补给导游的正式补佣金额。 */
    @TableField("manual_guide_bonus_amount")
    private BigDecimal manualGuideBonusAmount;

    /** 公司补佣说明。 */
    @TableField("manual_guide_bonus_remark")
    private String manualGuideBonusRemark;

    /** 公司返佣合计。 */
    @TableField("company_rebate_amount")
    private BigDecimal companyRebateAmount;

    /** 人头费合计。 */
    @TableField("head_fee_amount")
    private BigDecimal headFeeAmount;

    /** 内账公司购物利润。 */
    @TableField("internal_company_profit_amount")
    private BigDecimal internalCompanyProfitAmount;

    /** 外账公司购物利润。无发票购物佣金默认不进入外账。 */
    @TableField("external_company_profit_amount")
    private BigDecimal externalCompanyProfitAmount;

    /** 计算人。 */
    @TableField("calculated_by")
    private String calculatedBy;

    /** 计算时间。 */
    @TableField("calculated_at")
    private OffsetDateTime calculatedAt;

    /** 状态。active当前有效，superseded被新计算替代，cancelled作废。 */
    @TableField("status")
    private String status;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public String getRuleSource() { return ruleSource; }
    public void setRuleSource(String ruleSource) { this.ruleSource = ruleSource; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public BigDecimal getThresholdPerCapitaAmount() { return thresholdPerCapitaAmount; }
    public void setThresholdPerCapitaAmount(BigDecimal thresholdPerCapitaAmount) { this.thresholdPerCapitaAmount = thresholdPerCapitaAmount; }
    public BigDecimal getBaseCommissionRate() { return baseCommissionRate; }
    public void setBaseCommissionRate(BigDecimal baseCommissionRate) { this.baseCommissionRate = baseCommissionRate; }
    public BigDecimal getTargetCommissionRate() { return targetCommissionRate; }
    public void setTargetCommissionRate(BigDecimal targetCommissionRate) { this.targetCommissionRate = targetCommissionRate; }
    public String getLadderCalcMode() { return ladderCalcMode; }
    public void setLadderCalcMode(String ladderCalcMode) { this.ladderCalcMode = ladderCalcMode; }
    public BigDecimal getTotalConsumptionAmount() { return totalConsumptionAmount; }
    public void setTotalConsumptionAmount(BigDecimal totalConsumptionAmount) { this.totalConsumptionAmount = totalConsumptionAmount; }
    public BigDecimal getPerCapitaConsumptionAmount() { return perCapitaConsumptionAmount; }
    public void setPerCapitaConsumptionAmount(BigDecimal perCapitaConsumptionAmount) { this.perCapitaConsumptionAmount = perCapitaConsumptionAmount; }
    public Boolean getThresholdReached() { return thresholdReached; }
    public void setThresholdReached(Boolean thresholdReached) { this.thresholdReached = thresholdReached; }
    public BigDecimal getBaseGuideCommissionAmount() { return baseGuideCommissionAmount; }
    public void setBaseGuideCommissionAmount(BigDecimal baseGuideCommissionAmount) { this.baseGuideCommissionAmount = baseGuideCommissionAmount; }
    public BigDecimal getLadderExtraCommissionAmount() { return ladderExtraCommissionAmount; }
    public void setLadderExtraCommissionAmount(BigDecimal ladderExtraCommissionAmount) { this.ladderExtraCommissionAmount = ladderExtraCommissionAmount; }
    public BigDecimal getGuideCommissionTotalAmount() { return guideCommissionTotalAmount; }
    public void setGuideCommissionTotalAmount(BigDecimal guideCommissionTotalAmount) { this.guideCommissionTotalAmount = guideCommissionTotalAmount; }
    public BigDecimal getManualGuideBonusAmount() { return manualGuideBonusAmount; }
    public void setManualGuideBonusAmount(BigDecimal manualGuideBonusAmount) { this.manualGuideBonusAmount = manualGuideBonusAmount; }
    public String getManualGuideBonusRemark() { return manualGuideBonusRemark; }
    public void setManualGuideBonusRemark(String manualGuideBonusRemark) { this.manualGuideBonusRemark = manualGuideBonusRemark; }
    public BigDecimal getCompanyRebateAmount() { return companyRebateAmount; }
    public void setCompanyRebateAmount(BigDecimal companyRebateAmount) { this.companyRebateAmount = companyRebateAmount; }
    public BigDecimal getHeadFeeAmount() { return headFeeAmount; }
    public void setHeadFeeAmount(BigDecimal headFeeAmount) { this.headFeeAmount = headFeeAmount; }
    public BigDecimal getInternalCompanyProfitAmount() { return internalCompanyProfitAmount; }
    public void setInternalCompanyProfitAmount(BigDecimal internalCompanyProfitAmount) { this.internalCompanyProfitAmount = internalCompanyProfitAmount; }
    public BigDecimal getExternalCompanyProfitAmount() { return externalCompanyProfitAmount; }
    public void setExternalCompanyProfitAmount(BigDecimal externalCompanyProfitAmount) { this.externalCompanyProfitAmount = externalCompanyProfitAmount; }
    public String getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(String calculatedBy) { this.calculatedBy = calculatedBy; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
