package com.mtravel.platform.finance.guideimprest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 导游备用金申请请求。
 *
 * @param teamId 团队 ID
 * @param guideId 导游档案 ID
 * @param requestedAmount 本次申请发放金额
 * @param companyMarkupRate 本次公司加点率，按百分数保存
 * @param remark 申请备注
 */
public record GuideImprestApplyRequest(
        @NotNull Long teamId,
        @NotNull Long guideId,
        @DecimalMin(value = "0", inclusive = false, message = "申请备用金必须大于0") BigDecimal requestedAmount,
        @DecimalMin(value = "0", message = "公司加点率不能小于0") BigDecimal companyMarkupRate,
        @Size(max = 500) String remark
) {
    /** 兼容旧调用方，未传加点率时由后端读取系统默认配置。 */
    public GuideImprestApplyRequest(Long teamId, Long guideId, BigDecimal requestedAmount, String remark) {
        this(teamId, guideId, requestedAmount, null, remark);
    }
}
