package com.mtravel.platform.dispatch.guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 导游请假保存请求。
 *
 * @param guideId 导游 ID
 * @param startAt 请假开始时间
 * @param endAt 请假结束时间
 * @param leaveReason 请假原因
 * @param remark 备注
 */
public record GuideLeaveSaveRequest(
        @NotNull(message = "请选择导游") Long guideId,
        @NotNull(message = "请填写开始时间") LocalDateTime startAt,
        @NotNull(message = "请填写结束时间") LocalDateTime endAt,
        @NotBlank(message = "请填写请假原因") @Size(max = 300, message = "请假原因不能超过300字") String leaveReason,
        @Size(max = 1000, message = "备注不能超过1000字") String remark
) {
}
