package com.mtravel.platform.dispatch.guide.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 导游排班日历响应。
 *
 * @param startDate 开始日期
 * @param endDate 结束日期
 * @param dates 日期列
 * @param rows 导游排班行
 */
public record GuideScheduleCalendarResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<ScheduleDate> dates,
        List<GuideRow> rows
) {
    /** 日期列。 */
    public record ScheduleDate(LocalDate date, String label, String weekLabel) {
    }

    /** 单个导游的排班行。 */
    public record GuideRow(Long guideId, String guideName, String guideMobile, List<ScheduleBlock> blocks) {
    }

    /** 排班占用块。sourceType 为 team 或 leave。 */
    public record ScheduleBlock(
            String sourceType,
            Long sourceId,
            Long teamId,
            String teamNo,
            Long guideId,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String status
    ) {
    }
}
