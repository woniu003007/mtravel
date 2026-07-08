package com.mtravel.platform.finance.guideimprest.dto;

import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 导游备用金申请响应。
 */
public record GuideImprestResponse(
        Long id,
        String requestNo,
        Long teamId,
        String teamNo,
        String teamType,
        String businessType,
        LocalDate departureDate,
        Long departmentId,
        String departmentName,
        Long operatorEmployeeId,
        String operatorEmployeeName,
        Long guideId,
        String guideName,
        String guideMobile,
        Integer guestCount,
        BigDecimal companyMarkupRate,
        BigDecimal cashCostAmount,
        BigDecimal optionalDeductionAmount,
        BigDecimal calculatedAmount,
        BigDecimal suggestedImprestAmount,
        BigDecimal guideTurnInAmount,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal paidAmount,
        BigDecimal balanceAmount,
        String status,
        String applicant,
        OffsetDateTime appliedAt,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectedBy,
        OffsetDateTime rejectedAt,
        String approvalRemark,
        String cancelledBy,
        OffsetDateTime cancelledAt,
        String cancelReason,
        String remark,
        OffsetDateTime createdAt,
        BigDecimal occupiedAuthorizationAmount,
        BigDecimal availableAuthorizationAmount,
        Integer currentGuestCount,
        BigDecimal currentCashCostAmount,
        BigDecimal currentOptionalDeductionAmount,
        BigDecimal currentCalculatedAmount,
        BigDecimal currentSuggestedImprestAmount,
        BigDecimal currentGuideTurnInAmount,
        Boolean calculationChanged,
        String calculationChangeMessage,
        List<GuideImprestCalcLineResponse> calcLines
) {

    /** 将实体和明细转换为接口响应。 */
    public static GuideImprestResponse fromEntity(
            FinanceGuideImprestEntity entity,
            List<GuideImprestCalcLineResponse> calcLines
    ) {
        return fromEntity(entity, calcLines, null);
    }

    /** 将实体、明细和当前计算状态转换为接口响应。 */
    public static GuideImprestResponse fromEntity(
            FinanceGuideImprestEntity entity,
            List<GuideImprestCalcLineResponse> calcLines,
            CurrentCalculation currentCalculation
    ) {
        return new GuideImprestResponse(
                entity.getId(),
                entity.getRequestNo(),
                entity.getTeamId(),
                entity.getTeamNo(),
                entity.getTeamType(),
                entity.getBusinessType(),
                entity.getDepartureDate(),
                entity.getDepartmentId(),
                entity.getDepartmentName(),
                entity.getOperatorEmployeeId(),
                entity.getOperatorEmployeeName(),
                entity.getGuideId(),
                entity.getGuideName(),
                entity.getGuideMobile(),
                entity.getGuestCount(),
                entity.getCompanyMarkupRate(),
                entity.getCashCostAmount(),
                entity.getOptionalDeductionAmount(),
                entity.getCalculatedAmount(),
                entity.getSuggestedImprestAmount(),
                entity.getGuideTurnInAmount(),
                entity.getRequestedAmount(),
                entity.getApprovedAmount(),
                entity.getPaidAmount(),
                entity.getBalanceAmount(),
                entity.getStatus(),
                entity.getApplicant(),
                entity.getAppliedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectedBy(),
                entity.getRejectedAt(),
                entity.getApprovalRemark(),
                entity.getCancelledBy(),
                entity.getCancelledAt(),
                entity.getCancelReason(),
                entity.getRemark(),
                entity.getCreatedAt(),
                currentCalculation == null ? null : currentCalculation.occupiedAuthorizationAmount(),
                currentCalculation == null ? null : currentCalculation.availableAuthorizationAmount(),
                currentCalculation == null ? null : currentCalculation.currentGuestCount(),
                currentCalculation == null ? null : currentCalculation.currentCashCostAmount(),
                currentCalculation == null ? null : currentCalculation.currentOptionalDeductionAmount(),
                currentCalculation == null ? null : currentCalculation.currentCalculatedAmount(),
                currentCalculation == null ? null : currentCalculation.currentSuggestedImprestAmount(),
                currentCalculation == null ? null : currentCalculation.currentGuideTurnInAmount(),
                currentCalculation == null ? null : currentCalculation.calculationChanged(),
                currentCalculation == null ? null : currentCalculation.calculationChangeMessage(),
                calcLines
        );
    }

    /**
     * 当前团队安排重新计算后的备用金状态。
     *
     * @param occupiedAuthorizationAmount 已占用授权金额
     * @param availableAuthorizationAmount 当前还可申请授权金额
     * @param currentGuestCount 当前实收人数
     * @param currentCashCostAmount 当前现付总成本
     * @param currentOptionalDeductionAmount 当前自费抵扣金额
     * @param currentCalculatedAmount 当前原始计算结果
     * @param currentSuggestedImprestAmount 当前建议备用金
     * @param currentGuideTurnInAmount 当前导游应上交金额
     * @param calculationChanged 当前计算是否和申请快照不同
     * @param calculationChangeMessage 变化提示
     */
    public record CurrentCalculation(
            BigDecimal occupiedAuthorizationAmount,
            BigDecimal availableAuthorizationAmount,
            Integer currentGuestCount,
            BigDecimal currentCashCostAmount,
            BigDecimal currentOptionalDeductionAmount,
            BigDecimal currentCalculatedAmount,
            BigDecimal currentSuggestedImprestAmount,
            BigDecimal currentGuideTurnInAmount,
            Boolean calculationChanged,
            String calculationChangeMessage
    ) {
    }
}
