package com.mtravel.platform.purchase.resourcequote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 普通资源报价规则实体，对应 resource_quote_rules 表。
 *
 * <p>规则按资源类型和可选客户等级保存建议、最低比例及固定加价口径；客户等级为空表示该资源类型的默认规则。</p>
 */
@TableName("resource_quote_rules")
public class ResourceQuoteRuleEntity extends TenantSoftDeleteEntity {

    /** 资源类型，例如 hotel、scenic、vehicle。 */
    @TableField("resource_type")
    private String resourceType;

    /** 可选客户等级 ID；为空时表示不区分等级的默认规则。 */
    @TableField("customer_level_id")
    private Long customerLevelId;

    /** 建议比例上浮，按百分数保存，例如 20 表示 20%。 */
    @TableField("suggested_markup_rate")
    private BigDecimal suggestedMarkupRate;

    /** 最低比例上浮，按百分数保存。 */
    @TableField("minimum_markup_rate")
    private BigDecimal minimumMarkupRate;

    /** 建议固定加价，单位为元。 */
    @TableField("suggested_fixed_markup")
    private BigDecimal suggestedFixedMarkup;

    /** 最低固定加价，单位为元。 */
    @TableField("minimum_fixed_markup")
    private BigDecimal minimumFixedMarkup;

    /** 规则状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getCustomerLevelId() {
        return customerLevelId;
    }

    public void setCustomerLevelId(Long customerLevelId) {
        this.customerLevelId = customerLevelId;
    }

    public BigDecimal getSuggestedMarkupRate() {
        return suggestedMarkupRate;
    }

    public void setSuggestedMarkupRate(BigDecimal suggestedMarkupRate) {
        this.suggestedMarkupRate = suggestedMarkupRate;
    }

    public BigDecimal getMinimumMarkupRate() {
        return minimumMarkupRate;
    }

    public void setMinimumMarkupRate(BigDecimal minimumMarkupRate) {
        this.minimumMarkupRate = minimumMarkupRate;
    }

    public BigDecimal getSuggestedFixedMarkup() {
        return suggestedFixedMarkup;
    }

    public void setSuggestedFixedMarkup(BigDecimal suggestedFixedMarkup) {
        this.suggestedFixedMarkup = suggestedFixedMarkup;
    }

    public BigDecimal getMinimumFixedMarkup() {
        return minimumFixedMarkup;
    }

    public void setMinimumFixedMarkup(BigDecimal minimumFixedMarkup) {
        this.minimumFixedMarkup = minimumFixedMarkup;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
