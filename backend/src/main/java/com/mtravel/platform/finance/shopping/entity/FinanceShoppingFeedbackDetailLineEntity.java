package com.mtravel.platform.finance.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 购物反馈消费详情实体。
 *
 * <p>该表保存购物店反馈下的品类明细。总额返佣模式也会落一条“综合”明细，
 * 保证后续返佣、导游核对和统计只走同一套明细汇总口径。</p>
 */
@TableName("finance_shopping_feedback_detail_lines")
public class FinanceShoppingFeedbackDetailLineEntity extends TenantSoftDeleteEntity {

    /** 购物反馈父记录 ID。 */
    @TableField("feedback_line_id")
    private Long feedbackLineId;

    /** 团队 ID，冗余保存便于按团队批量查询。 */
    @TableField("team_id")
    private Long teamId;

    /** 购物品类。 */
    @TableField("category_name")
    private String categoryName;

    /** 当前品类进店人数，用于核对，不作为团队人均消费分母。 */
    @TableField("people_count")
    private Integer peopleCount;

    /** 人头费金额。 */
    @TableField("head_fee_amount")
    private BigDecimal headFeeAmount;

    /** 消费金额。 */
    @TableField("consumption_amount")
    private BigDecimal consumptionAmount;

    /** 公司返佣比例，百分数。 */
    @TableField("company_rebate_rate")
    private BigDecimal companyRebateRate;

    /** 公司返佣金额。 */
    @TableField("company_rebate_amount")
    private BigDecimal companyRebateAmount;

    /** 导游现场提成比例，百分数。 */
    @TableField("guide_commission_rate")
    private BigDecimal guideCommissionRate;

    /** 导游现场提成金额，仅用于业务核对。 */
    @TableField("guide_commission_amount")
    private BigDecimal guideCommissionAmount;

    /** 购物店现场现结金额，仅用于核对。 */
    @TableField("cash_amount")
    private BigDecimal cashAmount;

    /** 排序号。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getFeedbackLineId() { return feedbackLineId; }
    public void setFeedbackLineId(Long feedbackLineId) { this.feedbackLineId = feedbackLineId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public BigDecimal getHeadFeeAmount() { return headFeeAmount; }
    public void setHeadFeeAmount(BigDecimal headFeeAmount) { this.headFeeAmount = headFeeAmount; }
    public BigDecimal getConsumptionAmount() { return consumptionAmount; }
    public void setConsumptionAmount(BigDecimal consumptionAmount) { this.consumptionAmount = consumptionAmount; }
    public BigDecimal getCompanyRebateRate() { return companyRebateRate; }
    public void setCompanyRebateRate(BigDecimal companyRebateRate) { this.companyRebateRate = companyRebateRate; }
    public BigDecimal getCompanyRebateAmount() { return companyRebateAmount; }
    public void setCompanyRebateAmount(BigDecimal companyRebateAmount) { this.companyRebateAmount = companyRebateAmount; }
    public BigDecimal getGuideCommissionRate() { return guideCommissionRate; }
    public void setGuideCommissionRate(BigDecimal guideCommissionRate) { this.guideCommissionRate = guideCommissionRate; }
    public BigDecimal getGuideCommissionAmount() { return guideCommissionAmount; }
    public void setGuideCommissionAmount(BigDecimal guideCommissionAmount) { this.guideCommissionAmount = guideCommissionAmount; }
    public BigDecimal getCashAmount() { return cashAmount; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
