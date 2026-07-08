package com.mtravel.platform.finance.guideimprest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 导游备用金申请作废请求。
 *
 * @param cancelReason 作废原因
 */
public record GuideImprestCancelRequest(
        @NotBlank(message = "作废原因不能为空") @Size(max = 500) String cancelReason
) {
}
