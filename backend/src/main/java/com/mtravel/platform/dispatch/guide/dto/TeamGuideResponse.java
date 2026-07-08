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
 * @param approvedImprestAmount 累计已审批备用金金额
 * @param pendingImprestAmount 待审批备用金金额
 * @param paidImprestAmount 已付款备用金金额
 * @param imprestBalanceAmount 已审批未付款备用金余额
 * @param imprestApprovalStatus 备用金综合状态，none无申请，pending有待审批，approved_unpaid已批未付，partial_paid部分付款，paid已付清
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
        BigDecimal approvedImprestAmount,
        BigDecimal pendingImprestAmount,
        BigDecimal paidImprestAmount,
        BigDecimal imprestBalanceAmount,
        String imprestApprovalStatus,
        OffsetDateTime createdAt
) {
    /** 将实体转换为接口响应。 */
    public static TeamGuideResponse fromEntity(DispatchTeamGuideEntity entity) {
        return fromEntity(entity, ImprestSummary.empty());
    }

    /** 将实体和备用金申请汇总转换为接口响应。 */
    public static TeamGuideResponse fromEntity(DispatchTeamGuideEntity entity, ImprestSummary summary) {
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
                summary.approvedImprestAmount(),
                summary.pendingImprestAmount(),
                summary.paidImprestAmount(),
                summary.imprestBalanceAmount(),
                summary.imprestApprovalStatus(),
                entity.getCreatedAt()
        );
    }

    /**
     * 导游备用金申请汇总。
     *
     * @param approvedImprestAmount 累计已审批金额
     * @param pendingImprestAmount 待审批金额
     * @param paidImprestAmount 已付款金额
     * @param imprestBalanceAmount 已审批未付款余额
     * @param imprestApprovalStatus 综合状态
     */
    public record ImprestSummary(
            BigDecimal approvedImprestAmount,
            BigDecimal pendingImprestAmount,
            BigDecimal paidImprestAmount,
            BigDecimal imprestBalanceAmount,
            String imprestApprovalStatus
    ) {
        /** 空汇总。 */
        public static ImprestSummary empty() {
            BigDecimal zero = BigDecimal.ZERO.setScale(2);
            return new ImprestSummary(zero, zero, zero, zero, "none");
        }
    }
}
