package com.mtravel.platform.sales.team.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售团队主信息保存请求。
 *
 * <p>用于团期页面和团队操作页修改正式团队快照字段。团队快照字段只影响当前团队，
 * 不反写产品模板，避免已生成团队和后续团期互相污染。</p>
 */
public record SalesTeamSaveRequest(
        LocalDate departureDate,
        @Size(max = 20) String teamType,
        @Size(max = 120) String businessType,
        Long departmentId,
        @Size(max = 160) String departmentName,
        Long operatorEmployeeId,
        @Size(max = 100) String operatorEmployeeName,
        Long escortEmployeeId,
        @Size(max = 100) String escortEmployeeName,
        @Min(value = 0, message = "总位数不能小于0") Integer totalSeats,
        @DecimalMin(value = "0", message = "单房差不能小于0") BigDecimal singleRoomDifference,
        @Size(max = 500) String remark
) {}
