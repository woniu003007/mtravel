package com.mtravel.platform.finance.guideimprest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 导游备用金付款记录实体。
 *
 * <p>一张备用金申请可以分多次付款。真实银行现金流水后续可通过付款单或银行账引用本记录。</p>
 */
@TableName("finance_guide_imprest_payments")
public class FinanceGuideImprestPaymentEntity extends TenantSoftDeleteEntity {

    /** 备用金申请 ID。 */
    @TableField("imprest_id")
    private Long imprestId;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 付款编号。 */
    @TableField("payment_no")
    private String paymentNo;

    /** 付款日期。 */
    @TableField("payment_date")
    private LocalDate paymentDate;

    /** 付款方式。 */
    @TableField("payment_method")
    private String paymentMethod;

    /** 付款账户名称。 */
    @TableField("payment_account_name")
    private String paymentAccountName;

    /** 付款金额。 */
    @TableField("amount")
    private BigDecimal amount;

    /** 付款经办人。 */
    @TableField("payer")
    private String payer;

    /** 付款状态。 */
    @TableField("status")
    private String status;

    public Long getImprestId() { return imprestId; }
    public void setImprestId(Long imprestId) { this.imprestId = imprestId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentAccountName() { return paymentAccountName; }
    public void setPaymentAccountName(String paymentAccountName) { this.paymentAccountName = paymentAccountName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPayer() { return payer; }
    public void setPayer(String payer) { this.payer = payer; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
