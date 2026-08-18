package com.mtravel.platform.agent.customer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/** 客户 Agent 服务能力配置实体。 */
@TableName("agent_customer_service_settings")
public class AgentCustomerServiceSettingEntity extends TenantSoftDeleteEntity {

    @TableField("customer_id") private Long customerId;
    @TableField("service_state") private String serviceState;
    @TableField("product_access_mode") private String productAccessMode;
    @TableField("can_query_products") private Boolean canQueryProducts;
    @TableField("can_query_prices") private Boolean canQueryPrices;
    @TableField("can_query_policies") private Boolean canQueryPolicies;
    @TableField("can_create_quote_requests") private Boolean canCreateQuoteRequests;
    @TableField("can_create_handoffs") private Boolean canCreateHandoffs;
    @TableField("default_tax_included") private Boolean defaultTaxIncluded;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getServiceState() { return serviceState; }
    public void setServiceState(String serviceState) { this.serviceState = serviceState; }
    public String getProductAccessMode() { return productAccessMode; }
    public void setProductAccessMode(String productAccessMode) { this.productAccessMode = productAccessMode; }
    public Boolean getCanQueryProducts() { return canQueryProducts; }
    public void setCanQueryProducts(Boolean canQueryProducts) { this.canQueryProducts = canQueryProducts; }
    public Boolean getCanQueryPrices() { return canQueryPrices; }
    public void setCanQueryPrices(Boolean canQueryPrices) { this.canQueryPrices = canQueryPrices; }
    public Boolean getCanQueryPolicies() { return canQueryPolicies; }
    public void setCanQueryPolicies(Boolean canQueryPolicies) { this.canQueryPolicies = canQueryPolicies; }
    public Boolean getCanCreateQuoteRequests() { return canCreateQuoteRequests; }
    public void setCanCreateQuoteRequests(Boolean value) { this.canCreateQuoteRequests = value; }
    public Boolean getCanCreateHandoffs() { return canCreateHandoffs; }
    public void setCanCreateHandoffs(Boolean canCreateHandoffs) { this.canCreateHandoffs = canCreateHandoffs; }
    public Boolean getDefaultTaxIncluded() { return defaultTaxIncluded; }
    public void setDefaultTaxIncluded(Boolean defaultTaxIncluded) { this.defaultTaxIncluded = defaultTaxIncluded; }
}
