package com.mtravel.platform.finance.shopping.dto;

import com.mtravel.platform.finance.shopping.entity.FinanceShoppingFeedbackLineEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 购物店反馈明细响应。
 *
 * @param id 明细 ID
 * @param supplierId 购物店供应商 ID
 * @param shopName 购物店名称
 * @param guideId 导游 ID
 * @param guideName 导游姓名
 * @param businessDate 消费日期
 * @param peopleCount 进店人数
 * @param consumptionAmount 消费总额
 * @param companyRebateAmount 公司返佣金额
 * @param guideCommissionAmount 导游从购物店现场取得或应得的佣金金额，仅用于核对
 * @param headFeeAmount 人头费金额
 * @param rebateCalcMode 返佣计算模式
 * @param feedbackSource 反馈来源
 * @param remark 备注
 * @param detailLines 消费详情
 */
public record ShoppingFeedbackLineResponse(
        Long id,
        Long supplierId,
        String shopName,
        Long guideId,
        String guideName,
        LocalDate businessDate,
        Integer peopleCount,
        BigDecimal consumptionAmount,
        BigDecimal companyRebateAmount,
        BigDecimal guideCommissionAmount,
        BigDecimal headFeeAmount,
        String rebateCalcMode,
        String feedbackSource,
        String remark,
        List<ShoppingFeedbackDetailLineResponse> detailLines
) {

    /** 根据反馈实体生成响应。 */
    public static ShoppingFeedbackLineResponse fromEntity(FinanceShoppingFeedbackLineEntity entity) {
        return fromEntity(entity, List.of());
    }

    /** 根据反馈实体和消费详情生成响应。 */
    public static ShoppingFeedbackLineResponse fromEntity(
            FinanceShoppingFeedbackLineEntity entity,
            List<ShoppingFeedbackDetailLineResponse> detailLines
    ) {
        return new ShoppingFeedbackLineResponse(
                entity.getId(),
                entity.getSupplierId(),
                entity.getShopName(),
                entity.getGuideId(),
                entity.getGuideName(),
                entity.getBusinessDate(),
                entity.getPeopleCount(),
                entity.getConsumptionAmount(),
                entity.getCompanyRebateAmount(),
                entity.getGuideCommissionAmount(),
                entity.getHeadFeeAmount(),
                entity.getRebateCalcMode(),
                entity.getFeedbackSource(),
                entity.getRemark(),
                detailLines == null ? List.of() : detailLines
        );
    }
}
