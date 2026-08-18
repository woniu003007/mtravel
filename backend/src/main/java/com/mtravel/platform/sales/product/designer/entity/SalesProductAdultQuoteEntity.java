package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 产品成人报价草稿实体，对应 sales_product_adult_quotes 表。 */
@TableName("sales_product_adult_quotes")
public class SalesProductAdultQuoteEntity extends TenantSoftDeleteEntity {

    @TableField("product_id") private Long productId;
    @TableField("planned_adult_count") private Integer plannedAdultCount;
    @TableField("adult_cost_amount") private BigDecimal adultCostAmount;
    @TableField("markup_amount") private BigDecimal markupAmount;
    @TableField("adult_sale_amount") private BigDecimal adultSaleAmount;
    @TableField("valid_until") private LocalDate validUntil;
    @TableField("quote_remark") private String quoteRemark;
    @TableField("status") private String status;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getPlannedAdultCount() { return plannedAdultCount; }
    public void setPlannedAdultCount(Integer plannedAdultCount) { this.plannedAdultCount = plannedAdultCount; }
    public BigDecimal getAdultCostAmount() { return adultCostAmount; }
    public void setAdultCostAmount(BigDecimal adultCostAmount) { this.adultCostAmount = adultCostAmount; }
    public BigDecimal getMarkupAmount() { return markupAmount; }
    public void setMarkupAmount(BigDecimal markupAmount) { this.markupAmount = markupAmount; }
    public BigDecimal getAdultSaleAmount() { return adultSaleAmount; }
    public void setAdultSaleAmount(BigDecimal adultSaleAmount) { this.adultSaleAmount = adultSaleAmount; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public String getQuoteRemark() { return quoteRemark; }
    public void setQuoteRemark(String quoteRemark) { this.quoteRemark = quoteRemark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
