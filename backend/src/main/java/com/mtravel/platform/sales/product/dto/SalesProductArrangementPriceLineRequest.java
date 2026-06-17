package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 销售产品团队安排价格明细保存请求。
 *
 * <p>一条安排可以包含多条价格明细，例如住宿的标间和单房差，购物的品类消费和返佣明细。
 * 这里保存的是产品模板参考，不代表真实团队已经产生应付或导游报账。</p>
 *
 * @param projectId 费用项目ID，可关联企业资料费用项目
 * @param projectName 费用项目名称
 * @param unitPrice 单价
 * @param quantity 数量
 * @param amount 小计金额
 * @param salePrice 自费项目销售单价
 * @param costPrice 自费项目成本单价
 * @param cashAmount 现结金额
 * @param creditAmount 挂账金额
 * @param guideCommissionAmount 导游提成金额
 * @param guideCommissionRate 导游提成比例
 * @param companyRebateAmount 公司返佣金额
 * @param headFeeAmount 人头费金额
 * @param consumptionAmount 消费金额
 * @param sortOrder 排序号
 * @param remark 备注
 */
public record SalesProductArrangementPriceLineRequest(
        Long projectId,
        @Size(max = 120) String projectName,
        @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal unitPrice,
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,
        @DecimalMin(value = "0", message = "小计不能小于0") BigDecimal amount,
        @DecimalMin(value = "0", message = "销售价不能小于0") BigDecimal salePrice,
        @DecimalMin(value = "0", message = "成本价不能小于0") BigDecimal costPrice,
        @DecimalMin(value = "0", message = "现结金额不能小于0") BigDecimal cashAmount,
        @DecimalMin(value = "0", message = "挂账金额不能小于0") BigDecimal creditAmount,
        @DecimalMin(value = "0", message = "导游提成金额不能小于0") BigDecimal guideCommissionAmount,
        @DecimalMin(value = "0", message = "导游提成比例不能小于0") BigDecimal guideCommissionRate,
        @DecimalMin(value = "0", message = "公司返佣不能小于0") BigDecimal companyRebateAmount,
        @DecimalMin(value = "0", message = "人头费不能小于0") BigDecimal headFeeAmount,
        @DecimalMin(value = "0", message = "消费金额不能小于0") BigDecimal consumptionAmount,
        Integer sortOrder,
        String remark
) {}
