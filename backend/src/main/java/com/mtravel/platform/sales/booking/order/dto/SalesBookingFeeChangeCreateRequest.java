package com.mtravel.platform.sales.booking.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 收客订单费用变更新增请求。
 *
 * <p>前端金额输入保持正数，服务层根据变更方向转换为正负金额入库，避免前端和后端各自解释金额方向。</p>
 *
 * @param changeType 变更方向，increase 加收，decrease 退减。
 * @param feeProjectId 企业资料费用项目 ID，只允许附加费用类型。
 * @param feeDescription 费用说明。
 * @param amount 输入金额，必须大于 0。
 * @param remark 备注。
 */
public record SalesBookingFeeChangeCreateRequest(
        @NotBlank(message = "变更方向不能为空")
        @Pattern(regexp = "increase|decrease", message = "变更方向不合法")
        String changeType,
        @NotNull(message = "费用项目不能为空")
        Long feeProjectId,
        @NotBlank(message = "费用说明不能为空")
        @Size(max = 300, message = "费用说明不能超过300个字符")
        String feeDescription,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于0")
        BigDecimal amount,
        String remark
) {
}
