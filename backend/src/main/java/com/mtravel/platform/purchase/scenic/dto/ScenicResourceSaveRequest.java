package com.mtravel.platform.purchase.scenic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 景区资源保存请求。 */
public record ScenicResourceSaveRequest(
        @NotBlank(message = "景区名称不能为空") @Size(max = 200) String scenicName,
        @Size(max = 80) String city,
        @Size(max = 120) String area,
        @Size(max = 300) String address,
        @NotBlank(message = "票种不能为空") @Size(max = 120) String ticketType,
        Long supplierId,
        @DecimalMin(value = "0.00", message = "采购价不能小于0") BigDecimal purchasePrice,
        @DecimalMin(value = "0.00", message = "协议价不能小于0") BigDecimal agreementPrice,
        @Size(max = 40) String priceUnit,
        LocalDate validFrom,
        LocalDate validTo,
        String freeTicketRule,
        String halfTicketRule,
        @Size(max = 80) String contactName,
        @Size(max = 40) String contactPhone,
        @Pattern(regexp = "active|disabled", message = "景区资源状态不合法") String status,
        String remark
) {}
