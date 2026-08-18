package com.mtravel.platform.system.config.dto;

/**
 * 业务风控配置返回对象。
 *
 * @param customerRiskApprovalEnabled 客户合同到期或授信超限时是否启用客户授信审批。
 */
public record BusinessRiskConfigResponse(
        boolean customerRiskApprovalEnabled
) {
}
