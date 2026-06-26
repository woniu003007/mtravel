package com.mtravel.platform.sales.ordertransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 团队操作页拼团请求。
 *
 * @param orderIds 需要拼团的订单 ID 列表
 * @param targetTeamId 目标团队 ID。新系统固定多个来源订单只能拼到同一个目标团
 * @param tagFlag 是否打标
 * @param remark 通用备注
 * @param remarks 针对订单和目标团队的备注
 */
public record SalesOrderTransferMergeRequest(
        @NotEmpty(message = "请选择订单") List<Long> orderIds,
        @NotNull(message = "请选择目标团队") Long targetTeamId,
        boolean tagFlag,
        @Size(max = 1000, message = "备注不能超过1000个字符") String remark,
        @Valid List<SalesOrderTransferRemarkRequest> remarks
) {
}
