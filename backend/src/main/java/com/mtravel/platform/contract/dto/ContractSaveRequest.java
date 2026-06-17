package com.mtravel.platform.contract.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 统一合同保存请求。
 *
 * <p>客户合同与采购合同共用该协议；服务层根据 contractType 校验应绑定客户还是供应商。</p>
 */
public record ContractSaveRequest(
        @NotBlank
        @Pattern(regexp = "customer|scenic|hotel|restaurant|vehicle|traffic|other|ground_agent|guide|finance_fee|current_refund|extra_fee|shopping", message = "合同类型不合法")
        String contractType,
        Long customerId,
        Long supplierId,
        @Size(max = 80) String contractNo,
        @Size(max = 200) String contractName,
        @Size(max = 200) String counterpartyName,
        LocalDate startDate,
        LocalDate endDate,
        String settlementTerms,
        String purchasePriceSummary,
        @Size(max = 200) String legalSubject,
        @Size(max = 200) String invoiceSubject,
        @Size(max = 200) String settlementSubject,
        @Size(max = 120) String templateName,
        @Min(0) Integer reminderDays,
        Long attachmentId,
        String contractFileUrl,
        @Size(max = 40) String printStatus,
        @Pattern(regexp = "active|disabled|terminated", message = "合同状态不合法") String status,
        String remark,
        @Size(max = 200) String partyAName,
        @Size(max = 40) String partyAPhone,
        @Size(max = 40) String partyAFax,
        @Size(max = 300) String partyAAddress,
        @Size(max = 80) String partyAContact,
        @Size(max = 200) String partyBName,
        @Size(max = 40) String partyBPhone,
        @Size(max = 40) String partyBFax,
        @Size(max = 300) String partyBAddress,
        @Size(max = 80) String partyBContact,
        String agreementContent,
        String otherContent
) {
}
