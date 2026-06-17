package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductArrangementPriceLineEntity;
import java.math.BigDecimal;

/**
 * 销售产品团队安排价格明细返回对象。
 *
 * <p>用于团队安排模板页回显老系统“价格信息”中的多行项目。</p>
 */
public record SalesProductArrangementPriceLineResponse(
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
        BigDecimal headFeeAmount,
        BigDecimal consumptionAmount,
        Integer sortOrder,
        String remark
) {
    /** 将价格明细实体转换为接口返回对象。 */
    public static SalesProductArrangementPriceLineResponse fromEntity(SalesProductArrangementPriceLineEntity entity) {
        return new SalesProductArrangementPriceLineResponse(
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
                entity.getHeadFeeAmount(),
                entity.getConsumptionAmount(),
                entity.getSortOrder(),
                entity.getRemark()
        );
    }
}
