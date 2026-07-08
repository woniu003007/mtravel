package com.mtravel.platform.finance.guideimprest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 导游备用金付款登记请求。
 *
 * @param amount 本次付款金额
 * @param paymentDate 付款日期
 * @param paymentMethod 付款方式
 * @param paymentAccountName 付款账户名称
 * @param remark 付款备注
 */
public record GuideImprestPaymentRequest(
        @NotNull @DecimalMin(value = "0.01", message = "付款金额必须大于0") BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @Size(max = 40) String paymentMethod,
        @Size(max = 120) String paymentAccountName,
        @Size(max = 500) String remark
) {
}
