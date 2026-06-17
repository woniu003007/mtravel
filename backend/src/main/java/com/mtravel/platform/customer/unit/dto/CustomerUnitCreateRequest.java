package com.mtravel.platform.customer.unit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增客户单位请求。
 *
 * @param customerCode 客户编码或业务代码，允许为空；非空时同一租户下未删除记录不可重复。
 * @param customerName 客户单位名称，是客户主档最核心的业务名称。
 * @param categoryId 客户分类 ID，允许为空；传值时必须关联当前租户下启用分类。
 * @param creditLimit 客户实际授信额度，单位元，允许为空，空值按 0 处理。
 * @param province 客户所在地省份。
 * @param city 客户所在地城市。
 * @param district 客户所在地区县。
 * @param departmentName 客户归属部门名称。
 * @param dispatcherName 默认操作计调或默认操作人。
 * @param contactName 客户负责人或联系人姓名。
 * @param contactPhone 客户负责人或联系人电话。
 * @param registrarName 登记人名称。
 * @param contractExpireDate 客户合同有效期止，用于排团或下单提醒。
 * @param status 客户状态，只允许 active / disabled，允许为空，默认 active。
 * @param remark 客户备注。
 * @param departmentId 归属部门 ID，允许为空；为空表示全公司可见或暂未分配。
 * @param dispatcherEmployeeId 默认操作计调员工 ID，允许为空；为空表示未分配。
 * @param settlementMethod 结款方式，允许为空，默认 unlimited。
 * @param billStartDate 账单起始日期，用于后续账期提醒。
 * @param billDay 约定结款日，取值 1 到 31，允许为空。
 */
public record CustomerUnitCreateRequest(
        @Size(max = 64, message = "客户编码不能超过64个字符")
        String customerCode,
        @NotBlank(message = "客户名称不能为空")
        @Size(max = 200, message = "客户名称不能超过200个字符")
        String customerName,
        Long categoryId,
        @DecimalMin(value = "0.00", message = "客户授信额度不能小于0")
        BigDecimal creditLimit,
        @Size(max = 80, message = "省份不能超过80个字符")
        String province,
        @Size(max = 80, message = "城市不能超过80个字符")
        String city,
        @Size(max = 80, message = "区县不能超过80个字符")
        String district,
        @Size(max = 100, message = "部门名称不能超过100个字符")
        String departmentName,
        @Size(max = 80, message = "操作计调不能超过80个字符")
        String dispatcherName,
        @Size(max = 80, message = "负责人不能超过80个字符")
        String contactName,
        @Size(max = 40, message = "联系电话不能超过40个字符")
        String contactPhone,
        @Size(max = 80, message = "登记人不能超过80个字符")
        String registrarName,
        LocalDate contractExpireDate,
        @Pattern(regexp = "active|disabled", message = "客户状态只能是active或disabled")
        String status,
        String remark,
        Long departmentId,
        Long dispatcherEmployeeId,
        @Pattern(regexp = "unlimited|cash|monthly_1|monthly_2|monthly_3|monthly_4|monthly_5|monthly_6|monthly_7|monthly_8|monthly_9|monthly_10|monthly_11|monthly_12", message = "客户结款方式不合法")
        String settlementMethod,
        LocalDate billStartDate,
        @Min(value = 1, message = "结款日不能小于1")
        @Max(value = 31, message = "结款日不能大于31")
        Integer billDay
) {
}
