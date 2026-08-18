package com.mtravel.platform.system.config.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 业务风控配置保存请求。
 *
 * @param customerRiskApprovalEnabled 是否启用客户授信审批。
 */
public record BusinessRiskConfigUpdateRequest(
        @NotNull(message = "客户风控审批开关不能为空")
        Boolean customerRiskApprovalEnabled
) {
}
