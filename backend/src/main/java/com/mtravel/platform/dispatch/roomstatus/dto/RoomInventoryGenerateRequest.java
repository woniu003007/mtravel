package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 每日房态库存生成请求。
 *
 * <p>按酒店房型和日期生成总量库存，适配老系统“总量/余量”的房态管理方式。</p>
 */
public record RoomInventoryGenerateRequest(
        @NotBlank(message = "来源类型不能为空") String sourceType,
        @NotNull(message = "来源ID不能为空") Long sourceId,
        Long roomTypeId,
        @NotBlank(message = "房型不能为空") String roomType,
        @NotNull(message = "开始日期不能为空") LocalDate startDate,
        @NotNull(message = "结束日期不能为空") LocalDate endDate,
        @NotNull(message = "总量不能为空") @Min(value = 0, message = "总量不能小于0") Integer totalQuantity,
        String status
) {
}
