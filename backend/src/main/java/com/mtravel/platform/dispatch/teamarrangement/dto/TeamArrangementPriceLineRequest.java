package com.mtravel.platform.dispatch.teamarrangement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 正式团队安排价格明细保存请求。
 *
 * @param projectId 费用项目 ID
 * @param projectName 费用项目名称
 * @param unitPrice 单价
 * @param quantity 数量
 * @param amount 小计金额
 * @param salePrice 自费销售价
 * @param costPrice 自费成本价
 * @param cashAmount 现结金额
 * @param creditAmount 挂账金额
 * @param guideCommissionAmount 导游提成金额
 * @param guideCommissionRate 导游提成比例
 * @param companyRebateAmount 公司返佣金额
 * @param companyRebateRate 公司返佣比例
 * @param headFeeAmount 人头费金额
 * @param consumptionAmount 消费金额
 * @param sortOrder 排序号
 * @param remark 备注
 */
public record TeamArrangementPriceLineRequest(
        Long projectId,
        @Size(max = 120, message = "费用项目名称不能超过120字") String projectName,
        @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal unitPrice,
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,
        @DecimalMin(value = "0", message = "小计不能小于0") BigDecimal amount,
        @DecimalMin(value = "0", message = "销售价不能小于0") BigDecimal salePrice,
        @DecimalMin(value = "0", message = "成本价不能小于0") BigDecimal costPrice,
        @DecimalMin(value = "0", message = "现结金额不能小于0") BigDecimal cashAmount,
        @DecimalMin(value = "0", message = "挂账金额不能小于0") BigDecimal creditAmount,
        @DecimalMin(value = "0", message = "导游提成不能小于0") BigDecimal guideCommissionAmount,
        @DecimalMin(value = "0", message = "导游提成比例不能小于0") BigDecimal guideCommissionRate,
        @DecimalMin(value = "0", message = "公司返佣不能小于0") BigDecimal companyRebateAmount,
        @DecimalMin(value = "0", message = "公司返佣比例不能小于0") BigDecimal companyRebateRate,
        @DecimalMin(value = "0", message = "人头费不能小于0") BigDecimal headFeeAmount,
        @DecimalMin(value = "0", message = "消费金额不能小于0") BigDecimal consumptionAmount,
        Integer sortOrder,
        String remark
) {}
