package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售产品团队安排价格明细实体。
 *
 * <p>一条安排可包含多条价格明细，例如住宿房型、景区票种、自费分摊或购物消费品类。
 * 这些记录属于产品模板参考数据，不直接生成真实团队成本或财务应付。</p>
 */
@TableName("sales_product_arrangement_price_lines")
public class SalesProductArrangementPriceLineEntity extends TenantSoftDeleteEntity {

    /** 所属产品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 所属安排项 ID。 */
    @TableField("arrangement_item_id")
    private Long arrangementItemId;

    /** 费用项目 ID。 */
    @TableField("project_id")
    private Long projectId;

    /** 费用项目名称。 */
    @TableField("project_name")
    private String projectName;

    /** 单价。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 数量。 */
    @TableField("quantity")
    private BigDecimal quantity;

    /** 小计金额。 */
    @TableField("amount")
    private BigDecimal amount;

    /** 自费销售单价。 */
    @TableField("sale_price")
    private BigDecimal salePrice;

    /** 自费成本单价。 */
    @TableField("cost_price")
    private BigDecimal costPrice;

    /** 现结金额。 */
    @TableField("cash_amount")
    private BigDecimal cashAmount;

    /** 挂账金额。 */
    @TableField("credit_amount")
    private BigDecimal creditAmount;

    /** 导游提成金额。 */
    @TableField("guide_commission_amount")
    private BigDecimal guideCommissionAmount;

    /** 导游提成比例。 */
    @TableField("guide_commission_rate")
    private BigDecimal guideCommissionRate;

    /** 公司返佣金额。 */
    @TableField("company_rebate_amount")
    private BigDecimal companyRebateAmount;

    /** 公司返佣比例。 */
    @TableField("company_rebate_rate")
    private BigDecimal companyRebateRate;

    /** 人头费金额。 */
    @TableField("head_fee_amount")
    private BigDecimal headFeeAmount;

    /** 消费金额。 */
    @TableField("consumption_amount")
    private BigDecimal consumptionAmount;

    /** 排序号。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getArrangementItemId() { return arrangementItemId; }
    public void setArrangementItemId(Long arrangementItemId) { this.arrangementItemId = arrangementItemId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getCashAmount() { return cashAmount; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
    public BigDecimal getGuideCommissionAmount() { return guideCommissionAmount; }
    public void setGuideCommissionAmount(BigDecimal guideCommissionAmount) { this.guideCommissionAmount = guideCommissionAmount; }
    public BigDecimal getGuideCommissionRate() { return guideCommissionRate; }
    public void setGuideCommissionRate(BigDecimal guideCommissionRate) { this.guideCommissionRate = guideCommissionRate; }
    public BigDecimal getCompanyRebateAmount() { return companyRebateAmount; }
    public void setCompanyRebateAmount(BigDecimal companyRebateAmount) { this.companyRebateAmount = companyRebateAmount; }
    public BigDecimal getCompanyRebateRate() { return companyRebateRate; }
    public void setCompanyRebateRate(BigDecimal companyRebateRate) { this.companyRebateRate = companyRebateRate; }
    public BigDecimal getHeadFeeAmount() { return headFeeAmount; }
    public void setHeadFeeAmount(BigDecimal headFeeAmount) { this.headFeeAmount = headFeeAmount; }
    public BigDecimal getConsumptionAmount() { return consumptionAmount; }
    public void setConsumptionAmount(BigDecimal consumptionAmount) { this.consumptionAmount = consumptionAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
