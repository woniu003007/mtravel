package com.mtravel.platform.enterprise.bankaccount.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业银行账号实体。
 *
 * <p>该表保存企业可用于收款、付款、确认件打印和员工现金账授权的账户资料。
 * 银行卡、支付宝、微信、客户现付、房券等都按企业收付款账户统一维护。</p>
 */
@TableName("enterprise_bank_accounts")
public class EnterpriseBankAccountEntity extends TenantSoftDeleteEntity {

    /** 开户行或账户类型，例如中国农业银行、支付宝、微信、客户现付。 */
    @TableField("bank_name")
    private String bankName;

    /** 户名或收款主体名称。 */
    @TableField("account_name")
    private String accountName;

    /** 银行账号、支付宝账号、微信标识或内部结算方式标识。 */
    @TableField("account_no")
    private String accountNo;

    /** 是否在打印单据或确认件中展示。 */
    @TableField("print_enabled")
    private Boolean printEnabled;

    /** 其它账户说明，例如银行地址、SWIFT、联行号、境外汇款资料。 */
    @TableField("other_info")
    private String otherInfo;

    /** 账户状态。active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Boolean getPrintEnabled() {
        return printEnabled;
    }

    public void setPrintEnabled(Boolean printEnabled) {
        this.printEnabled = printEnabled;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public void setOtherInfo(String otherInfo) {
        this.otherInfo = otherInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
