package com.mtravel.platform.sales.ordertransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 团队操作页拼团请求。
 *
 * @param orderIds 需要拼团的订单 ID 列表
 * @param targetTeamIds 目标团队 ID 列表。按老系统矩阵拼团口径，可一次选择多个目标团
 * @param tagFlag 是否打标
 * @param remark 通用备注
 * @param items 每个“来源订单 x 目标团队”组合的确认页填写项
 * @param remarks 旧版针对订单和目标团队的备注，保留用于兼容历史前端
 */
public record SalesOrderTransferMergeRequest(
        @NotEmpty(message = "请选择订单") List<Long> orderIds,
        List<Long> targetTeamIds,
        boolean tagFlag,
        @Size(max = 1000, message = "备注不能超过1000个字符") String remark,
        @Valid List<SalesOrderTransferMergeItemRequest> items,
        @Valid List<SalesOrderTransferRemarkRequest> remarks
) {
}
