package com.mtravel.platform.dispatch.teamarrangement.dto;

import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import java.math.BigDecimal;

/**
 * 正式团队安排价格明细响应。
 */
public record TeamArrangementPriceLineResponse(
        Long id,
        Long projectId,
        String projectName,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal amount,
        BigDecimal salePrice,
        BigDecimal costPrice,
        BigDecimal cashAmount,
        BigDecimal creditAmount,
        BigDecimal guideCommissionAmount,
        BigDecimal guideCommissionRate,
        BigDecimal companyRebateAmount,
        BigDecimal companyRebateRate,
        BigDecimal headFeeAmount,
        BigDecimal consumptionAmount,
        Integer sortOrder,
        String remark
) {
    /** 转换价格明细实体为响应对象。 */
    public static TeamArrangementPriceLineResponse fromEntity(DispatchTeamArrangementPriceLineEntity entity) {
        return new TeamArrangementPriceLineResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getProjectName(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getAmount(),
                entity.getSalePrice(),
                entity.getCostPrice(),
                entity.getCashAmount(),
                entity.getCreditAmount(),
                entity.getGuideCommissionAmount(),
                entity.getGuideCommissionRate(),
                entity.getCompanyRebateAmount(),
                entity.getCompanyRebateRate(),
                entity.getHeadFeeAmount(),
                entity.getConsumptionAmount(),
                entity.getSortOrder(),
                entity.getRemark()
        );
    }
}
