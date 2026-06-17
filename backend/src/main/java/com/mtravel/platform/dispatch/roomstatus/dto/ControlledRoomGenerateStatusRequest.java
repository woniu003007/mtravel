package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 生成自控房源每日房态请求。
 *
 * <p>生成房态只会补齐缺失日期，不覆盖已锁定、占用、维修或保留状态，避免误释放业务占用。</p>
 */
public record ControlledRoomGenerateStatusRequest(
        @NotNull(message = "自控房源不能为空") Long resourceId,
        @NotNull(message = "开始日期不能为空") LocalDate startDate,
        @NotNull(message = "结束日期不能为空") LocalDate endDate
) {
}
