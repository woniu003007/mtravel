package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 自控房源锁房请求。
 *
 * <p>锁房是排房确认前的预占动作。请求可以一次锁定多个房号，Service 会逐日校验房态，
 * 只允许可用房间被锁定。</p>
 */
public record ControlledRoomLockRequest(
        @NotNull(message = "自控房源不能为空") Long resourceId,
        @NotEmpty(message = "请选择要锁定的房间") List<Long> roomIds,
        @NotNull(message = "入住日期不能为空") LocalDate checkInDate,
        @NotNull(message = "退房日期不能为空") LocalDate checkOutDate,
        @Size(max = 80) String teamNo,
        @Size(max = 200) String teamName,
        @Size(max = 80) String requiredStandard,
        String remark
) {
}
