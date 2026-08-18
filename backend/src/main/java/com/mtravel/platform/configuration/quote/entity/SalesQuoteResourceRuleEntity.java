package com.mtravel.platform.configuration.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 普通资源销售报价规则实体。
 */
@TableName("sales_quote_resource_rules")
public class SalesQuoteResourceRuleEntity extends TenantSoftDeleteEntity {

    /** 资源类型，购物不进入本规则。 */
    @TableField("resource_type")
    private String resourceType;

    /** 客户等级或分类 ID，空值表示默认规则。 */
    @TableField(value = "customer_category_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long customerCategoryId;

    /** 客户等级或分类名称快照。 */
    @TableField(value = "customer_category_name", updateStrategy = FieldStrategy.ALWAYS)
    private String customerCategoryName;

    /** 允许使用的报价方式。 */
    @TableField("quote_mode")
    private String quoteMode;

    /** 建议比例上浮。 */
    @TableField("suggested_markup_rate")
    private BigDecimal suggestedMarkupRate;

    /** 最低比例上浮。 */
    @TableField("minimum_markup_rate")
    private BigDecimal minimumMarkupRate;

    /** 建议固定加价。 */
    @TableField("suggested_fixed_markup")
    private BigDecimal suggestedFixedMarkup;

    /** 最低固定加价。 */
    @TableField("minimum_fixed_markup")
    private BigDecimal minimumFixedMarkup;

    /** 规则状态。 */
    @TableField("status")
    private String status;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getCustomerCategoryId() { return customerCategoryId; }
    public void setCustomerCategoryId(Long customerCategoryId) { this.customerCategoryId = customerCategoryId; }
    public String getCustomerCategoryName() { return customerCategoryName; }
    public void setCustomerCategoryName(String customerCategoryName) { this.customerCategoryName = customerCategoryName; }
    public String getQuoteMode() { return quoteMode; }
    public void setQuoteMode(String quoteMode) { this.quoteMode = quoteMode; }
    public BigDecimal getSuggestedMarkupRate() { return suggestedMarkupRate; }
    public void setSuggestedMarkupRate(BigDecimal suggestedMarkupRate) { this.suggestedMarkupRate = suggestedMarkupRate; }
    public BigDecimal getMinimumMarkupRate() { return minimumMarkupRate; }
    public void setMinimumMarkupRate(BigDecimal minimumMarkupRate) { this.minimumMarkupRate = minimumMarkupRate; }
    public BigDecimal getSuggestedFixedMarkup() { return suggestedFixedMarkup; }
    public void setSuggestedFixedMarkup(BigDecimal suggestedFixedMarkup) { this.suggestedFixedMarkup = suggestedFixedMarkup; }
    public BigDecimal getMinimumFixedMarkup() { return minimumFixedMarkup; }
    public void setMinimumFixedMarkup(BigDecimal minimumFixedMarkup) { this.minimumFixedMarkup = minimumFixedMarkup; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
