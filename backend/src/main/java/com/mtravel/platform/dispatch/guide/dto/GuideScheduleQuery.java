package com.mtravel.platform.dispatch.guide.dto;

import java.time.LocalDate;

/**
 * 导游排班日历查询条件。
 *
 * @param guideName 导游姓名关键字
 * @param startDate 开始日期
 */
public record GuideScheduleQuery(
        String guideName,
        LocalDate startDate
) {
}
