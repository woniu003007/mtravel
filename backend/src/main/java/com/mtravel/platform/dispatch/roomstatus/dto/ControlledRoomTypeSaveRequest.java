package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 自营房型保存请求。
 *
 * <p>房型承载标间、大床房、三人间等库存和价格口径，避免同一家酒店因为多个房型被重复建档。</p>
 */
public record ControlledRoomTypeSaveRequest(
        Long resourceId,
        @Size(max = 120, message = "房型名称不能超过120个字符") String roomType,
        @Size(max = 80, message = "床型不能超过80个字符") String bedType,
        Integer capacity,
        @DecimalMin(value = "0", message = "采购价不能小于0") BigDecimal purchasePrice,
        @DecimalMin(value = "0", message = "协议价不能小于0") BigDecimal agreementPrice,
        @Size(max = 40, message = "价格单位不能超过40个字符") String priceUnit,
        @Pattern(regexp = "active|disabled", message = "房型状态不合法") String status,
        String remark
) {
}
