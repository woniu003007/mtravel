package com.mtravel.platform.dispatch.guide.dto;

/**
 * 导游可用性响应。
 *
 * @param guideId 导游档案 ID
 * @param guideName 导游姓名
 * @param guideMobile 导游手机号
 * @param gender 性别
 * @param available 当前时间段是否可出团
 * @param unavailableType 不能出团类型，team 表示已有团队安排，leave 表示已批准请假
 * @param unavailableReason 不能出团原因
 */
public record GuideAvailabilityResponse(
        Long guideId,
        String guideName,
        String guideMobile,
        String gender,
        Boolean available,
        String unavailableType,
        String unavailableReason
) {
}
