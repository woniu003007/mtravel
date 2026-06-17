package com.mtravel.platform.contract.dto;

import com.mtravel.platform.contract.entity.ContractEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 统一合同接口返回对象，完整返回合同类型、关联主体、合同内容和附件信息。 */
public record ContractResponse(
        Long id, String contractType, Long customerId, Long supplierId, String contractNo,
        String contractName, String counterpartyName, LocalDate startDate, LocalDate endDate,
        String settlementTerms, String purchasePriceSummary, String legalSubject, String invoiceSubject,
        String settlementSubject, String templateName, Integer reminderDays, Long attachmentId,
        String contractFileUrl, String printStatus, String status, String remark,
        String partyAName, String partyAPhone, String partyAFax, String partyAAddress, String partyAContact,
        String partyBName, String partyBPhone, String partyBFax, String partyBAddress, String partyBContact,
        String agreementContent, String otherContent, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    /** 将统一合同实体转换为稳定的接口返回结构。 */
    public static ContractResponse fromEntity(ContractEntity entity) {
        return new ContractResponse(
                entity.getId(), entity.getContractType(), entity.getCustomerId(), entity.getSupplierId(),
                entity.getContractNo(), entity.getContractName(), entity.getCounterpartyName(),
                entity.getStartDate(), entity.getEndDate(), entity.getSettlementTerms(),
                entity.getPurchasePriceSummary(), entity.getLegalSubject(), entity.getInvoiceSubject(),
                entity.getSettlementSubject(), entity.getTemplateName(), entity.getReminderDays(),
                entity.getAttachmentId(), entity.getContractFileUrl(), entity.getPrintStatus(),
                entity.getStatus(), entity.getRemark(), entity.getPartyAName(), entity.getPartyAPhone(),
                entity.getPartyAFax(), entity.getPartyAAddress(), entity.getPartyAContact(),
                entity.getPartyBName(), entity.getPartyBPhone(), entity.getPartyBFax(),
                entity.getPartyBAddress(), entity.getPartyBContact(), entity.getAgreementContent(),
                entity.getOtherContent(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
