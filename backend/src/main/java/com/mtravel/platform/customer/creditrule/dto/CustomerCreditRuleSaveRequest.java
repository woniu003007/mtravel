package com.mtravel.platform.customer.creditrule.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 客户授信规则保存请求。
 *
 * @param customerLevelId 客户等级 ID，关联客户分类字典
 * @param creditLimit 默认授信额度，单位为元
 * @param paymentTermDays 允许账期天数，映射数据库 account_period_days
 * @param allowOverLimit 是否允许超过可用授信额度
 * @param approverEmployeeIds 按审批顺序选择的员工 ID 列表
 * @param ccEmployeeIds 接收审批通知的员工 ID 列表
 * @param status 规则状态，active 或 disabled
 * @param remark 规则备注
 */
public record CustomerCreditRuleSaveRequest(
        @NotNull(message = "客户等级不能为空") @Positive(message = "客户等级不合法") Long customerLevelId,
        @DecimalMin(value = "0.00", message = "授信额度不能小于0")
        @Digits(integer = 12, fraction = 2, message = "授信额度最多12位整数和2位小数") BigDecimal creditLimit,
        @Min(value = 0, message = "账期天数不能小于0") Integer paymentTermDays,
        Boolean allowOverLimit,
        @Size(max = 50, message = "审批员工不能超过50人") List<@NotNull @Positive Long> approverEmployeeIds,
        @Size(max = 50, message = "抄送员工不能超过50人") List<@NotNull @Positive Long> ccEmployeeIds,
        @Pattern(regexp = "active|disabled", message = "客户授信规则状态不合法") String status,
        String remark
) {
}
