package com.mtravel.platform.dispatch.vehicleusage.dto;

import com.mtravel.platform.dispatch.vehicleusage.entity.VehicleUsageHistoryEntity;

/**
 * 用车历史候选响应。
 *
 * @param id 候选记录 ID
 * @param historyType 候选类型
 * @param content 展示内容
 * @param usageCount 使用次数
 */
public record VehicleUsageHistoryResponse(
        Long id,
        String historyType,
        String content,
        Integer usageCount
) {

    /** 将实体转换为前端下拉候选。 */
    public static VehicleUsageHistoryResponse fromEntity(VehicleUsageHistoryEntity entity) {
        return new VehicleUsageHistoryResponse(
                entity.getId(),
                entity.getHistoryType(),
                entity.getContent(),
                entity.getUsageCount()
        );
    }
}
