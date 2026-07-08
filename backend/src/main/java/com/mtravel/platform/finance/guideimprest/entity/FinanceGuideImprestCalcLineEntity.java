package com.mtravel.platform.finance.guideimprest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 导游备用金计算明细实体。
 *
 * <p>每次申请都会保存计算快照，后续团队安排、自费价格或人数变化不会覆盖历史申请依据。</p>
 */
@TableName("finance_guide_imprest_calc_lines")
public class FinanceGuideImprestCalcLineEntity extends TenantSoftDeleteEntity {

    /** 备用金申请 ID。 */
    @TableField("imprest_id")
    private Long imprestId;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 明细类型。cash_cost 现付成本，optional_deduction 自费加点抵扣。 */
    @TableField("line_type")
    private String lineType;

    /** 来源团队安排 ID。 */
    @TableField("source_arrangement_id")
    private Long sourceArrangementId;

    /** 来源价格明细 ID。 */
    @TableField("source_price_line_id")
    private Long sourcePriceLineId;

    /** 团队安排类型。 */
    @TableField("arrangement_type")
    private String arrangementType;

    /** 项目名称。 */
    @TableField("item_name")
    private String itemName;

    /** 自费售价。 */
    @TableField("sale_price")
    private BigDecimal salePrice;

    /** 自费成本。 */
    @TableField("cost_price")
    private BigDecimal costPrice;

    /** 导游提成金额。 */
    @TableField("guide_commission_amount")
    private BigDecimal guideCommissionAmount;

    /** 导游提成比例。 */
    @TableField("guide_commission_rate")
    private BigDecimal guideCommissionRate;

    /** 导游提成计算方式。 */
    @TableField("guide_commission_calc_type")
    private String guideCommissionCalcType;

    /** 公司规定加点率。 */
    @TableField("company_markup_rate")
    private BigDecimal companyMarkupRate;

    /** 团队实收人数。 */
    @TableField("guest_count")
    private Integer guestCount;

    /** 本行金额。 */
    @TableField("amount")
    private BigDecimal amount;

    /** 排序号。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getImprestId() { return imprestId; }
    public void setImprestId(Long imprestId) { this.imprestId = imprestId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getLineType() { return lineType; }
    public void setLineType(String lineType) { this.lineType = lineType; }
    public Long getSourceArrangementId() { return sourceArrangementId; }
    public void setSourceArrangementId(Long sourceArrangementId) { this.sourceArrangementId = sourceArrangementId; }
    public Long getSourcePriceLineId() { return sourcePriceLineId; }
    public void setSourcePriceLineId(Long sourcePriceLineId) { this.sourcePriceLineId = sourcePriceLineId; }
    public String getArrangementType() { return arrangementType; }
    public void setArrangementType(String arrangementType) { this.arrangementType = arrangementType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getGuideCommissionAmount() { return guideCommissionAmount; }
    public void setGuideCommissionAmount(BigDecimal guideCommissionAmount) { this.guideCommissionAmount = guideCommissionAmount; }
    public BigDecimal getGuideCommissionRate() { return guideCommissionRate; }
    public void setGuideCommissionRate(BigDecimal guideCommissionRate) { this.guideCommissionRate = guideCommissionRate; }
    public String getGuideCommissionCalcType() { return guideCommissionCalcType; }
    public void setGuideCommissionCalcType(String guideCommissionCalcType) { this.guideCommissionCalcType = guideCommissionCalcType; }
    public BigDecimal getCompanyMarkupRate() { return companyMarkupRate; }
    public void setCompanyMarkupRate(BigDecimal companyMarkupRate) { this.companyMarkupRate = companyMarkupRate; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
