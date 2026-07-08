package com.mtravel.platform.dispatch.teamarrangement.dto;

import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementSectionStatusEntity;

/**
 * 正式团队安排分类流程状态返回对象。
 */
public record TeamArrangementSectionStatusResponse(
        Long id,
        Long teamId,
        String teamNo,
        String arrangementType,
        String status
) {
    /** 将实体转换为接口返回对象。 */
    public static TeamArrangementSectionStatusResponse fromEntity(DispatchTeamArrangementSectionStatusEntity entity) {
        return new TeamArrangementSectionStatusResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getTeamNo(),
                entity.getArrangementType(),
                entity.getStatus()
        );
    }
}
