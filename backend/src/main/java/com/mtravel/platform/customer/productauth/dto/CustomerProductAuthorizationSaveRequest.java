package com.mtravel.platform.customer.productauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 客户产品授权保存请求。 */
public record CustomerProductAuthorizationSaveRequest(
        @NotNull(message = "客户不能为空") Long customerId,
        @Size(max = 80) String productCode,
        @NotBlank(message = "产品名称不能为空") @Size(max = 200) String productName,
        LocalDate authorizedStartDate,
        LocalDate authorizedEndDate,
        @Pattern(regexp = "active|suspended|expired", message = "授权状态不合法") String authorizationStatus,
        String saleScope,
        String remark
) {}
