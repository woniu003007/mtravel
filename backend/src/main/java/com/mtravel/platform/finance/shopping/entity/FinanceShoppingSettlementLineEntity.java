package com.mtravel.platform.finance.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 购物业绩结算明细实体。
 *
 * <p>该表把结算快照拆到购物店反馈行，便于财务和购物返佣统计追溯每家店的数据。</p>
 */
@TableName("finance_shopping_settlement_lines")
public class FinanceShoppingSettlementLineEntity extends TenantSoftDeleteEntity {

    /** 结算快照 ID。 */
    @TableField("settlement_id")
    private Long settlementId;

    /** 团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 购物反馈明细 ID。 */
    @TableField("feedback_line_id")
    private Long feedbackLineId;

    /** 供应商 ID。 */
    @TableField("supplier_id")
    private Long supplierId;

    /** 购物店名称。 */
    @TableField("shop_name")
    private String shopName;

    /** 消费日期。 */
    @TableField("business_date")
    private LocalDate businessDate;

    /** 进店人数。 */
    @TableField("people_count")
    private Integer peopleCount;

    /** 消费总额。 */
    @TableField("consumption_amount")
    private BigDecimal consumptionAmount;

    /** 公司返佣金额。 */
    @TableField("company_rebate_amount")
    private BigDecimal companyRebateAmount;

    /** 导游从购物店现场取得或应得的佣金金额，仅用于业务核对。 */
    @TableField("guide_commission_amount")
    private BigDecimal guideCommissionAmount;

    /** 人头费金额。 */
    @TableField("head_fee_amount")
    private BigDecimal headFeeAmount;

    /** 明细行内部公司利润，按人头费加公司返佣计算。 */
    @TableField("line_company_profit_amount")
    private BigDecimal lineCompanyProfitAmount;

    /** 排序。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getFeedbackLineId() { return feedbackLineId; }
    public void setFeedbackLineId(Long feedbackLineId) { this.feedbackLineId = feedbackLineId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public BigDecimal getConsumptionAmount() { return consumptionAmount; }
    public void setConsumptionAmount(BigDecimal consumptionAmount) { this.consumptionAmount = consumptionAmount; }
    public BigDecimal getCompanyRebateAmount() { return companyRebateAmount; }
    public void setCompanyRebateAmount(BigDecimal companyRebateAmount) { this.companyRebateAmount = companyRebateAmount; }
    public BigDecimal getGuideCommissionAmount() { return guideCommissionAmount; }
    public void setGuideCommissionAmount(BigDecimal guideCommissionAmount) { this.guideCommissionAmount = guideCommissionAmount; }
    public BigDecimal getHeadFeeAmount() { return headFeeAmount; }
    public void setHeadFeeAmount(BigDecimal headFeeAmount) { this.headFeeAmount = headFeeAmount; }
    public BigDecimal getLineCompanyProfitAmount() { return lineCompanyProfitAmount; }
    public void setLineCompanyProfitAmount(BigDecimal lineCompanyProfitAmount) { this.lineCompanyProfitAmount = lineCompanyProfitAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
