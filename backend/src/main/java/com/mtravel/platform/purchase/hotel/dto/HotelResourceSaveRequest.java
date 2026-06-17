package com.mtravel.platform.purchase.hotel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 酒店资源保存请求。 */
public record HotelResourceSaveRequest(
        @NotBlank(message = "酒店名称不能为空") @Size(max = 200) String hotelName,
        @Size(max = 80) String city,
        @Size(max = 120) String area,
        @Size(max = 300) String address,
        @Size(max = 80) String starStandard,
        @NotBlank(message = "房型不能为空") @Size(max = 120) String roomType,
        Long supplierId,
        @DecimalMin(value = "0.00", message = "采购价不能小于0") BigDecimal purchasePrice,
        @DecimalMin(value = "0.00", message = "协议价不能小于0") BigDecimal agreementPrice,
        @Size(max = 40) String priceUnit,
        LocalDate validFrom,
        LocalDate validTo,
        @Size(max = 80) String contactName,
        @Size(max = 40) String contactPhone,
        @Pattern(regexp = "active|disabled", message = "酒店资源状态不合法") String status,
        String remark
) {}
