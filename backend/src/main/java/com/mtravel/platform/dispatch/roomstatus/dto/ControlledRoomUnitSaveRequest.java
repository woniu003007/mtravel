package com.mtravel.platform.dispatch.roomstatus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 自控房间明细保存请求。
 */
public record ControlledRoomUnitSaveRequest(
        @NotNull(message = "自控房源不能为空") Long resourceId,
        @Size(max = 80) String buildingName,
        @Size(max = 40) String floorNo,
        @NotBlank(message = "房号不能为空") @Size(max = 80) String roomNo,
        @Size(max = 120) String roomType,
        @Size(max = 80) String bedType,
        @Min(value = 0, message = "可住人数不能小于0") Integer capacity,
        @Pattern(regexp = "active|disabled|maintenance", message = "房间状态不合法") String status,
        String remark
) {
}
