package com.mtravel.platform.dispatch.guide.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团队导游安排保存请求。
 *
 * @param guideId 导游档案 ID
 * @param guideFee 导服费
 * @param imprestAmount 备用金金额
 * @param operationFee 操作费
 * @param startAt 上团时间
 * @param endAt 下团时间
 * @param feeMemo 费用说明
 * @param guideMemo 导游备注
 * @param tentative 是否待定中
 */
public record TeamGuideSaveRequest(
        @NotNull(message = "请选择导游") Long guideId,
        @DecimalMin(value = "0", message = "导服费不能小于0") BigDecimal guideFee,
        @DecimalMin(value = "0", message = "备用金不能小于0") BigDecimal imprestAmount,
        @DecimalMin(value = "0", message = "操作费不能小于0") BigDecimal operationFee,
        @NotNull(message = "请填写上团时间") LocalDateTime startAt,
        @NotNull(message = "请填写下团时间") LocalDateTime endAt,
        @Size(max = 1000, message = "费用说明不能超过1000字") String feeMemo,
        @Size(max = 1000, message = "导游备注不能超过1000字") String guideMemo,
        Boolean tentative
) {
}
