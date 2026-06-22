package com.mtravel.platform.dispatch.vehicleusage.dto;

import jakarta.validation.constraints.Size;

/**
 * 用车历史候选使用记录请求。
 *
 * @param historyType 候选类型，driver_info 或 vehicle_plate
 * @param content 本次手动输入内容
 */
public record VehicleUsageHistoryRecordRequest(
        String historyType,
        @Size(max = 160, message = "历史候选内容不能超过160个字符") String content
) {
}
