package com.mtravel.platform.customer.credit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * 客户授信账户保存请求。
 *
 * @param customerId 客户单位ID
 * @param creditLimit 授信额度
 * @param occupiedAmount 已占用额度，首版支持手工维护
 * @param pendingApprovalAmount 审批中额度
 * @param warningThreshold 预警阈值
 * @param overLimitAction 超限处理方式，none/remind/approval
 * @param status 状态，active/disabled
 * @param remark 备注
 */
public record CustomerCreditAccountSaveRequest(
        @NotNull(message = "客户不能为空") Long customerId,
        @DecimalMin(value = "0.00", message = "授信额度不能小于0") BigDecimal creditLimit,
        @DecimalMin(value = "0.00", message = "已占用额度不能小于0") BigDecimal occupiedAmount,
        @DecimalMin(value = "0.00", message = "审批中额度不能小于0") BigDecimal pendingApprovalAmount,
        @DecimalMin(value = "0.00", message = "预警阈值不能小于0") BigDecimal warningThreshold,
        @Pattern(regexp = "none|remind|approval", message = "超限处理方式不合法") String overLimitAction,
        @Pattern(regexp = "active|disabled", message = "状态只能是active或disabled") String status,
        String remark
) {}
