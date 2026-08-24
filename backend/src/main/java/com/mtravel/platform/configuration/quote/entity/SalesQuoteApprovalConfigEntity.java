package com.mtravel.platform.configuration.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 销售报价统一审批模式配置实体。
 */
@TableName("sales_quote_approval_configs")
public class SalesQuoteApprovalConfigEntity extends TenantSoftDeleteEntity {

    /** 审批模式：department_manager 部门负责人，specified_person 指定人员。 */
    @TableField("approval_mode")
    private String approvalMode;

    public String getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(String approvalMode) {
        this.approvalMode = approvalMode;
    }

}
