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
 * 修改客户单位请求。
 *
 * <p>修改时要求前端传入客户主档的完整核心字段，避免部分字段漏传导致数据库内容被误清空。</p>
 */
public record CustomerUnitUpdateRequest(
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
