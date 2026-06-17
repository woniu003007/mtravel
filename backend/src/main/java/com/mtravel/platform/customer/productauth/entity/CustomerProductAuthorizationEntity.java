package com.mtravel.platform.customer.productauth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;

/**
 * 客户产品授权实体，对应 customer_product_authorizations 表。
 *
 * <p>授权记录用于控制客户可售或可下单产品范围。首版只做台账维护，销售下单校验后续接入。</p>
 */
@TableName("customer_product_authorizations")
public class CustomerProductAuthorizationEntity extends TenantSoftDeleteEntity {
    @TableField("customer_id") private Long customerId;
    @TableField("product_code") private String productCode;
    @TableField("product_name") private String productName;
    @TableField("authorized_start_date") private LocalDate authorizedStartDate;
    @TableField("authorized_end_date") private LocalDate authorizedEndDate;
    @TableField("authorization_status") private String authorizationStatus;
    @TableField("sale_scope") private String saleScope;
    public Long getCustomerId(){return customerId;} public void setCustomerId(Long customerId){this.customerId=customerId;}
    public String getProductCode(){return productCode;} public void setProductCode(String productCode){this.productCode=productCode;}
    public String getProductName(){return productName;} public void setProductName(String productName){this.productName=productName;}
    public LocalDate getAuthorizedStartDate(){return authorizedStartDate;} public void setAuthorizedStartDate(LocalDate authorizedStartDate){this.authorizedStartDate=authorizedStartDate;}
    public LocalDate getAuthorizedEndDate(){return authorizedEndDate;} public void setAuthorizedEndDate(LocalDate authorizedEndDate){this.authorizedEndDate=authorizedEndDate;}
    public String getAuthorizationStatus(){return authorizationStatus;} public void setAuthorizationStatus(String authorizationStatus){this.authorizationStatus=authorizationStatus;}
    public String getSaleScope(){return saleScope;} public void setSaleScope(String saleScope){this.saleScope=saleScope;}
}
