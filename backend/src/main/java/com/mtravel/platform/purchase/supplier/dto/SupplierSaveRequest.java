package com.mtravel.platform.purchase.supplier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 供应商保存请求。 */
public record SupplierSaveRequest(
        @Size(max = 80) String supplierCode,
        @NotBlank(message = "供应商名称不能为空") @Size(max = 200) String supplierName,
        @Pattern(regexp = "hotel|scenic|vehicle|restaurant|traffic|ground_agent|shopping|other|common", message = "供应商分类不合法") String supplierCategory,
        Long buyerId,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @Size(max = 100) String settlementMethod,
        @Size(max = 4000, message = "基础信息不能超过4000个字符") String basicInfo,
        @Size(max = 80) String contactName,
        @Size(max = 40) String contactPhone,
        @Size(max = 40) String faxNumber,
        @Size(max = 240) String officeAddress,
        @Size(max = 160) String agreementName,
        @Min(0) @Max(5) Integer rating,
        @Pattern(regexp = "active|disabled|blacklisted", message = "供应商状态不合法") String status,
        String remark
) {}
