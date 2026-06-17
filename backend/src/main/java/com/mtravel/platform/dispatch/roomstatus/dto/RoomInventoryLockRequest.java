package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 按房型数量锁房请求。
 *
 * <p>计调排房先锁“某房型几间”，不强制选择具体房号。自营房号可后续再分配。</p>
 */
public record RoomInventoryLockRequest(
        @NotBlank(message = "来源类型不能为空") String sourceType,
        @NotNull(message = "来源ID不能为空") Long sourceId,
        Long roomTypeId,
        @NotBlank(message = "房型不能为空") String roomType,
        @NotNull(message = "入住日期不能为空") LocalDate checkInDate,
        @NotNull(message = "退房日期不能为空") LocalDate checkOutDate,
        @NotNull(message = "锁房数量不能为空") @Min(value = 1, message = "锁房数量必须大于0") Integer quantity,
        String teamNo,
        String teamName,
        String requiredStandard,
        String remark
) {
}
