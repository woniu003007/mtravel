package com.mtravel.platform.contract.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;

/**
 * 统一合同实体，对应 contracts 表。
 *
 * <p>客户销售合同和各类采购合同共用一张台账。客户、供应商只允许绑定一方，
 * 甲乙方信息按签署时内容保存快照，主档后续变化不会覆盖历史合同。</p>
 */
@TableName("contracts")
public class ContractEntity extends TenantSoftDeleteEntity {
    @TableField("contract_type") private String contractType;
    @TableField("customer_id") private Long customerId;
    @TableField("supplier_id") private Long supplierId;
    @TableField("contract_no") private String contractNo;
    @TableField("contract_name") private String contractName;
    @TableField("counterparty_name") private String counterpartyName;
    @TableField("start_date") private LocalDate startDate;
    @TableField("end_date") private LocalDate endDate;
    @TableField("settlement_terms") private String settlementTerms;
    @TableField("purchase_price_summary") private String purchasePriceSummary;
    @TableField("legal_subject") private String legalSubject;
    @TableField("invoice_subject") private String invoiceSubject;
    @TableField("settlement_subject") private String settlementSubject;
    @TableField("template_name") private String templateName;
    @TableField("reminder_days") private Integer reminderDays;
    @TableField("attachment_id") private Long attachmentId;
    @TableField("contract_file_url") private String contractFileUrl;
    @TableField("print_status") private String printStatus;
    @TableField("status") private String status;
    @TableField("party_a_name") private String partyAName;
    @TableField("party_a_phone") private String partyAPhone;
    @TableField("party_a_fax") private String partyAFax;
    @TableField("party_a_address") private String partyAAddress;
    @TableField("party_a_contact") private String partyAContact;
    @TableField("party_b_name") private String partyBName;
    @TableField("party_b_phone") private String partyBPhone;
    @TableField("party_b_fax") private String partyBFax;
    @TableField("party_b_address") private String partyBAddress;
    @TableField("party_b_contact") private String partyBContact;
    @TableField("agreement_content") private String agreementContent;
    @TableField("other_content") private String otherContent;

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getContractName() { return contractName; }
    public void setContractName(String contractName) { this.contractName = contractName; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getSettlementTerms() { return settlementTerms; }
    public void setSettlementTerms(String settlementTerms) { this.settlementTerms = settlementTerms; }
    public String getPurchasePriceSummary() { return purchasePriceSummary; }
    public void setPurchasePriceSummary(String purchasePriceSummary) { this.purchasePriceSummary = purchasePriceSummary; }
    public String getLegalSubject() { return legalSubject; }
    public void setLegalSubject(String legalSubject) { this.legalSubject = legalSubject; }
    public String getInvoiceSubject() { return invoiceSubject; }
    public void setInvoiceSubject(String invoiceSubject) { this.invoiceSubject = invoiceSubject; }
    public String getSettlementSubject() { return settlementSubject; }
    public void setSettlementSubject(String settlementSubject) { this.settlementSubject = settlementSubject; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Integer getReminderDays() { return reminderDays; }
    public void setReminderDays(Integer reminderDays) { this.reminderDays = reminderDays; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getContractFileUrl() { return contractFileUrl; }
    public void setContractFileUrl(String contractFileUrl) { this.contractFileUrl = contractFileUrl; }
    public String getPrintStatus() { return printStatus; }
    public void setPrintStatus(String printStatus) { this.printStatus = printStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPartyAName() { return partyAName; }
    public void setPartyAName(String partyAName) { this.partyAName = partyAName; }
    public String getPartyAPhone() { return partyAPhone; }
    public void setPartyAPhone(String partyAPhone) { this.partyAPhone = partyAPhone; }
    public String getPartyAFax() { return partyAFax; }
    public void setPartyAFax(String partyAFax) { this.partyAFax = partyAFax; }
    public String getPartyAAddress() { return partyAAddress; }
    public void setPartyAAddress(String partyAAddress) { this.partyAAddress = partyAAddress; }
    public String getPartyAContact() { return partyAContact; }
    public void setPartyAContact(String partyAContact) { this.partyAContact = partyAContact; }
    public String getPartyBName() { return partyBName; }
    public void setPartyBName(String partyBName) { this.partyBName = partyBName; }
    public String getPartyBPhone() { return partyBPhone; }
    public void setPartyBPhone(String partyBPhone) { this.partyBPhone = partyBPhone; }
    public String getPartyBFax() { return partyBFax; }
    public void setPartyBFax(String partyBFax) { this.partyBFax = partyBFax; }
    public String getPartyBAddress() { return partyBAddress; }
    public void setPartyBAddress(String partyBAddress) { this.partyBAddress = partyBAddress; }
    public String getPartyBContact() { return partyBContact; }
    public void setPartyBContact(String partyBContact) { this.partyBContact = partyBContact; }
    public String getAgreementContent() { return agreementContent; }
    public void setAgreementContent(String agreementContent) { this.agreementContent = agreementContent; }
    public String getOtherContent() { return otherContent; }
    public void setOtherContent(String otherContent) { this.otherContent = otherContent; }
}
