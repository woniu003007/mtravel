package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 产品成人报价草稿保存请求。 */
public record ProductDesignerAdultQuoteSaveRequest(
        Long id,
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "成人数不能为空") @Min(value = 1, message = "成人数必须大于0") Integer plannedAdultCount,
        @DecimalMin(value = "0", message = "加价金额不能小于0") BigDecimal markupAmount,
        @DecimalMin(value = "0", message = "成人对外价不能小于0") BigDecimal adultSaleAmount,
        LocalDate validUntil,
        String quoteRemark,
        @Pattern(regexp = "draft|confirmed", message = "报价状态不合法") String status
) {}
