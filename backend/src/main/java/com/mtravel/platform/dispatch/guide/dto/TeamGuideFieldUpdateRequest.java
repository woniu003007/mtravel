package com.mtravel.platform.dispatch.guide.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 团队导游安排单字段更新请求。
 *
 * @param field 字段名
 * @param value 字段值，后端按字段类型转换
 */
public record TeamGuideFieldUpdateRequest(
        @NotBlank(message = "字段名不能为空") String field,
        String value
) {
}
