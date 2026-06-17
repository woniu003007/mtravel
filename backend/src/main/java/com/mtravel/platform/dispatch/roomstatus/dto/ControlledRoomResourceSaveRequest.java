package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 自控房源批次保存请求。
 *
 * <p>自控房源保存的是自营酒店档案。房型、床型和价格放到房型管理中维护，
 * 避免同一家酒店因为标间、大床房、三人间被重复建档。</p>
 */
public record ControlledRoomResourceSaveRequest(
        @NotBlank(message = "酒店名称不能为空") @Size(max = 200) String hotelName,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 120) String district,
        @Size(max = 120) String area,
        @Size(max = 300) String address,
        @Size(max = 80) String starStandard,
        @Size(max = 120) String roomType,
        @Size(max = 200) String sourceName,
        @DecimalMin(value = "0.00", message = "采购价不能小于0") BigDecimal purchasePrice,
        @DecimalMin(value = "0.00", message = "协议价不能小于0") BigDecimal agreementPrice,
        @Size(max = 40) String priceUnit,
        LocalDate validFrom,
        LocalDate validTo,
        @Size(max = 80) String contactName,
        @Size(max = 40) String contactPhone,
        @Pattern(regexp = "active|disabled|expired", message = "自控房源状态不合法") String status,
        String remark
) {
}
