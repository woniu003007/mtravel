package com.mtravel.platform.finance.shopping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * 购物反馈消费详情保存请求。
 *
 * @param id 明细 ID，当前保存采用整体替换，前端可不传
 * @param categoryName 购物品类，总额模式由后端固定为“综合”
 * @param peopleCount 当前品类进店人数，用于核对，不参与团队人均消费分母
 * @param headFeeAmount 人头费金额
 * @param consumptionAmount 消费金额
 * @param companyRebateRate 公司返佣比例，百分数
 * @param companyRebateAmount 公司返佣金额
 * @param guideCommissionRate 导游现场提成比例，百分数
 * @param guideCommissionAmount 导游现场提成金额，仅用于业务核对
 * @param cashAmount 购物店现场现结金额，仅用于核对
 * @param remark 备注
 * @param sortOrder 排序号
 */
public record ShoppingFeedbackDetailLineSaveRequest(
        Long id,
        String categoryName,
        @Min(value = 0, message = "进店人数不能小于0") Integer peopleCount,
        @DecimalMin(value = "0", message = "人头费不能小于0") BigDecimal headFeeAmount,
        @DecimalMin(value = "0", message = "消费金额不能小于0") BigDecimal consumptionAmount,
        @DecimalMin(value = "0", message = "公司返佣比例不能小于0") BigDecimal companyRebateRate,
        @DecimalMin(value = "0", message = "公司返佣不能小于0") BigDecimal companyRebateAmount,
        @DecimalMin(value = "0", message = "导游提成比例不能小于0") BigDecimal guideCommissionRate,
        @DecimalMin(value = "0", message = "导游现场佣金不能小于0") BigDecimal guideCommissionAmount,
        @DecimalMin(value = "0", message = "现结金额不能小于0") BigDecimal cashAmount,
        String remark,
        Integer sortOrder
) {
}
