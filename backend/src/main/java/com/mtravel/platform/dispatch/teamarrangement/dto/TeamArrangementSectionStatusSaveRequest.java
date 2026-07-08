package com.mtravel.platform.dispatch.teamarrangement.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存正式团队安排分类流程状态请求。
 *
 * @param status 分类状态，pending 未完成，none 无需，done 完成
 */
public record TeamArrangementSectionStatusSaveRequest(
        @NotBlank(message = "分类状态不能为空")
        String status
) {
}
