package com.mtravel.platform.sales.team.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售团期批量生成请求。
 *
 * <p>支持按起止日期和星期批量生成团期，也支持老系统“特定团期”那种指定多个不连续日期生成。
 * 每个命中的日期生成一条团队主记录和一条默认价格行。</p>
 *
 * @param startDate 开始发团日期
 * @param endDate 结束发团日期
 * @param weekdays 允许生成的星期，使用 Java DayOfWeek 值，1 到 7
 * @param operatorEmployeeId 操作计调员工 ID
 * @param operatorEmployeeName 操作计调员工姓名快照
 * @param totalSeats 总位数
 * @param singleRoomDifference 单人房差价格
 * @param adultPrice 成人价格
 * @param childPrice 儿童价格
 * @param childNoBedPrice 儿童不占床价格
 * @param seniorPrice 老人价格
 * @param extraFee 附加费用
 * @param customerCategoryId 客户分类 ID，空值表示默认客户类型
 * @param customerCategoryName 客户类型名称
 * @param dates 特定团期日期列表；有值时优先按该列表生成，不再按起止日期和星期推算
 */
public record SalesTeamBatchCreateRequest(
        @NotNull(message = "开始日期不能为空") LocalDate startDate,
        @NotNull(message = "结束日期不能为空") LocalDate endDate,
        List<@Min(1) Integer> weekdays,
        Long operatorEmployeeId,
        @Size(max = 100) String operatorEmployeeName,
        @Min(value = 0, message = "总位数不能小于0") Integer totalSeats,
        @DecimalMin(value = "0", message = "单房差不能小于0") BigDecimal singleRoomDifference,
        @DecimalMin(value = "0", message = "成人价格不能小于0") BigDecimal adultPrice,
        @DecimalMin(value = "0", message = "儿童价格不能小于0") BigDecimal childPrice,
        @DecimalMin(value = "0", message = "儿童不占床价格不能小于0") BigDecimal childNoBedPrice,
        @DecimalMin(value = "0", message = "老人价格不能小于0") BigDecimal seniorPrice,
        @DecimalMin(value = "0", message = "附加费用不能小于0") BigDecimal extraFee,
        Long customerCategoryId,
        @Size(max = 120) String customerCategoryName,
        List<@NotNull(message = "特定团期日期不能为空") LocalDate> dates
) {}
