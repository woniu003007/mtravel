package com.mtravel.platform.sales.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 销售团队状态批量变更请求。
 *
 * @param teamIds 需要变更状态的团队 ID 列表
 * @param action 动作：stop、start、cancel、recover、delete
 * @param remark 状态变更说明
 */
public record SalesTeamStatusChangeRequest(
        List<Long> teamIds,
        @NotBlank(message = "状态动作不能为空")
        @Pattern(regexp = "stop|start|cancel|recover|delete", message = "状态动作不合法")
        String action,
        String remark
) {}
