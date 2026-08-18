package com.mtravel.platform.configuration.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 地接整团打包报价规则实体。
 */
@TableName("sales_quote_ground_agent_rules")
public class SalesQuoteGroundAgentRuleEntity extends TenantSoftDeleteEntity {

    /** 适用最小人数。 */
    @TableField("min_people")
    private Integer minPeople;

    /** 适用最大人数。 */
    @TableField("max_people")
    private Integer maxPeople;

    /** 整团打包价。 */
    @TableField("group_package_price")
    private BigDecimal groupPackagePrice;

    /** 规则状态。 */
    @TableField("status")
    private String status;

    public Integer getMinPeople() { return minPeople; }
    public void setMinPeople(Integer minPeople) { this.minPeople = minPeople; }
    public Integer getMaxPeople() { return maxPeople; }
    public void setMaxPeople(Integer maxPeople) { this.maxPeople = maxPeople; }
    public BigDecimal getGroupPackagePrice() { return groupPackagePrice; }
    public void setGroupPackagePrice(BigDecimal groupPackagePrice) { this.groupPackagePrice = groupPackagePrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
