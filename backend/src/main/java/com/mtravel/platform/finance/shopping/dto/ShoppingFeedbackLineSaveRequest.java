package com.mtravel.platform.finance.shopping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 购物店反馈明细保存请求。
 *
 * @param id 明细 ID，空表示新增
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
 * @param remark 备注
 * @param rebateCalcMode 返佣计算模式。total总额返佣，category按品类返佣
 * @param detailLines 消费详情。总额模式可为空，后端自动生成“综合”明细
 */
public record ShoppingFeedbackLineSaveRequest(
        Long id,
        Long supplierId,
        @NotBlank(message = "购物店名称不能为空") String shopName,
        Long guideId,
        String guideName,
        LocalDate businessDate,
        @Min(value = 0, message = "进店人数不能小于0") Integer peopleCount,
        @DecimalMin(value = "0", message = "消费总额不能小于0") BigDecimal consumptionAmount,
        @DecimalMin(value = "0", message = "公司返佣不能小于0") BigDecimal companyRebateAmount,
        @DecimalMin(value = "0", message = "导游现场佣金不能小于0") BigDecimal guideCommissionAmount,
        @DecimalMin(value = "0", message = "人头费不能小于0") BigDecimal headFeeAmount,
        String remark,
        String rebateCalcMode,
        List<ShoppingFeedbackDetailLineSaveRequest> detailLines
) {
}
