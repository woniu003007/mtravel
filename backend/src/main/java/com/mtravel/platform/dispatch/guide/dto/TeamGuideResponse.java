package com.mtravel.platform.dispatch.guide.dto;

import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 团队导游安排响应。
 *
 * @param id 安排 ID
 * @param teamId 团队 ID
 * @param teamNo 团号快照
 * @param guideId 导游 ID
 * @param guideName 导游姓名
 * @param guideMobile 导游手机
 * @param guideFee 导服费
 * @param imprestAmount 备用金
 * @param operationFee 操作费
 * @param startAt 上团时间
 * @param endAt 下团时间
 * @param feeMemo 费用说明
 * @param guideMemo 导游备注
 * @param tentative 是否待定中
 * @param status 安排状态
 * @param createdAt 创建时间
 */
public record TeamGuideResponse(
        Long id,
        Long teamId,
        String teamNo,
        Long guideId,
        String guideName,
        String guideMobile,
        BigDecimal guideFee,
        BigDecimal imprestAmount,
        BigDecimal operationFee,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String feeMemo,
        String guideMemo,
        Boolean tentative,
        String status,
        OffsetDateTime createdAt
) {
    /** 将实体转换为接口响应。 */
    public static TeamGuideResponse fromEntity(DispatchTeamGuideEntity entity) {
        return new TeamGuideResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getTeamNo(),
                entity.getGuideId(),
                entity.getGuideName(),
                entity.getGuideMobile(),
                entity.getGuideFee(),
                entity.getImprestAmount(),
                entity.getOperationFee(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getFeeMemo(),
                entity.getGuideMemo(),
                entity.getIsTentative(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
