package com.mtravel.platform.sales.ordertransfer.dto;

import java.util.List;

/**
 * 拼团执行结果。
 *
 * <p>矩阵拼团中，已存在的来源订单和目标团队组合按老系统口径跳过，其它组合继续生成目标团子订单。
 * 返回创建和跳过数量，便于前端给出可追溯提示。</p>
 *
 * @param createdCount 本次新生成的拼团子订单数量
 * @param skippedCount 因重复或无效自拼而跳过的组合数量
 * @param skippedItems 被跳过的组合
 */
public record SalesOrderTransferMergeResult(
        int createdCount,
        int skippedCount,
        List<SkippedItem> skippedItems
) {
    /** 被跳过的拼团组合。 */
    public record SkippedItem(Long orderId, Long targetTeamId, String reason) {
    }
}
