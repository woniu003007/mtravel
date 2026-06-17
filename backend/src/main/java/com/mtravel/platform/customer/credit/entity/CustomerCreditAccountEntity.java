package com.mtravel.platform.customer.credit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 客户授信账户实体，对应 customer_credit_accounts 表。
 *
 * <p>首版支持授信台账维护和手工录入已占用、审批中额度；订单自动占用属于后续销售联动。</p>
 */
@TableName("customer_credit_accounts")
public class CustomerCreditAccountEntity extends TenantSoftDeleteEntity {
    /** 客户单位 ID。 */
    @TableField("customer_id") private Long customerId;
    /** 授信额度。 */
    @TableField("credit_limit") private BigDecimal creditLimit;
    /** 已占用额度。 */
    @TableField("occupied_amount") private BigDecimal occupiedAmount;
    /** 审批中额度。 */
    @TableField("pending_approval_amount") private BigDecimal pendingApprovalAmount;
    /** 可用额度，由数据库生成列计算。 */
    @TableField(value = "available_amount", insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER, updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private BigDecimal availableAmount;
    /** 预警阈值。 */
    @TableField("warning_threshold") private BigDecimal warningThreshold;
    /** 超限处理方式：none/remind/approval。 */
    @TableField("over_limit_action") private String overLimitAction;
    /** 授信账户状态：active/disabled。 */
    @TableField("status") private String status;
    public Long getCustomerId(){return customerId;} public void setCustomerId(Long customerId){this.customerId=customerId;}
    public BigDecimal getCreditLimit(){return creditLimit;} public void setCreditLimit(BigDecimal creditLimit){this.creditLimit=creditLimit;}
    public BigDecimal getOccupiedAmount(){return occupiedAmount;} public void setOccupiedAmount(BigDecimal occupiedAmount){this.occupiedAmount=occupiedAmount;}
    public BigDecimal getPendingApprovalAmount(){return pendingApprovalAmount;} public void setPendingApprovalAmount(BigDecimal pendingApprovalAmount){this.pendingApprovalAmount=pendingApprovalAmount;}
    public BigDecimal getAvailableAmount(){return availableAmount;} public void setAvailableAmount(BigDecimal availableAmount){this.availableAmount=availableAmount;}
    public BigDecimal getWarningThreshold(){return warningThreshold;} public void setWarningThreshold(BigDecimal warningThreshold){this.warningThreshold=warningThreshold;}
    public String getOverLimitAction(){return overLimitAction;} public void setOverLimitAction(String overLimitAction){this.overLimitAction=overLimitAction;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
