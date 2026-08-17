package com.mtravel.platform.customer.creditrule.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 客户授信规则实体，对应 customer_credit_rules 表。
 *
 * <p>规则按客户分类维护默认额度、账期及超额审批配置。审批和抄送人员在库内按顺序保存为英文逗号分隔的员工 ID，
 * 对外接口再转换为 JSON 数组。</p>
 */
@TableName("customer_credit_rules")
public class CustomerCreditRuleEntity extends TenantSoftDeleteEntity {

    /** 客户等级 ID，关联 customer_categories。 */
    @TableField("customer_level_id")
    private Long customerLevelId;

    /** 该等级的默认授信额度，单位为元。 */
    @TableField("credit_limit")
    private BigDecimal creditLimit;

    /** 允许账期天数，数据库字段名沿用 account_period_days。 */
    @TableField("account_period_days")
    private Integer accountPeriodDays;

    /** 是否允许订单金额超过可用授信额度。 */
    @TableField("allow_over_limit")
    private Boolean allowOverLimit;

    /** 审批员工 ID 列表，按审批顺序以英文逗号分隔保存。 */
    @TableField("approver_employee_ids")
    private String approverEmployeeIds;

    /** 抄送员工 ID 列表，以英文逗号分隔保存。 */
    @TableField("cc_employee_ids")
    private String ccEmployeeIds;

    /** 规则状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public Long getCustomerLevelId() {
        return customerLevelId;
    }

    public void setCustomerLevelId(Long customerLevelId) {
        this.customerLevelId = customerLevelId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Integer getAccountPeriodDays() {
        return accountPeriodDays;
    }

    public void setAccountPeriodDays(Integer accountPeriodDays) {
        this.accountPeriodDays = accountPeriodDays;
    }

    public Boolean getAllowOverLimit() {
        return allowOverLimit;
    }

    public void setAllowOverLimit(Boolean allowOverLimit) {
        this.allowOverLimit = allowOverLimit;
    }

    public String getApproverEmployeeIds() {
        return approverEmployeeIds;
    }

    public void setApproverEmployeeIds(String approverEmployeeIds) {
        this.approverEmployeeIds = approverEmployeeIds;
    }

    public String getCcEmployeeIds() {
        return ccEmployeeIds;
    }

    public void setCcEmployeeIds(String ccEmployeeIds) {
        this.ccEmployeeIds = ccEmployeeIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
