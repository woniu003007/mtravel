package com.mtravel.platform.enterprise.expenseitem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 费用项目保存请求。
 *
 * @param resourceType 资源类型，用于控制项目适用范围
 * @param projectName 项目名称，例如成人、标间、标准餐
 * @param statisticsEnabled 是否参与统计
 * @param sortOrder 排序号
 * @param status 状态，active 启用，disabled 停用
 * @param remark 备注说明
 */
public record EnterpriseExpenseItemSaveRequest(
        @Pattern(
                regexp = "hotel|scenic|vehicle|restaurant|traffic|ground_agent|guide|finance_fee|current_refund|extra_fee|shopping|ticket|misc|other",
                message = "资源类型不合法"
        )
        String resourceType,
        @NotBlank(message = "项目名称不能为空") @Size(max = 120) String projectName,
        Boolean statisticsEnabled,
        @Min(value = 0, message = "排序号不能小于0") Integer sortOrder,
        @Pattern(regexp = "active|disabled", message = "费用项目状态不合法") String status,
        String remark
) {}
