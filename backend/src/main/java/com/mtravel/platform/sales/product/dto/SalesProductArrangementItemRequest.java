package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售产品团队安排参数保存请求。
 *
 * @param arrangementType 安排类型，例如住宿、用车、景区
 * @param itemName 安排项目名称
 * @param arrangementContent 安排内容或默认说明
 * @param quantity 默认数量
 * @param unitPrice 默认单价或费用参考
 * @param unitName 计量单位
 * @param settlementType 结算类型，cash 现结，credit 挂账
 * @param remark 备注
 * @param allocationMode 费用归属模式，group_order_average 全团/订单均摊，multi_order_average 多订单均摊成本
 * @param scheduleStartDay 开始或使用日期
 * @param scheduleEndDay 结束日期
 * @param departurePlace 出发地
 * @param arrivalPlace 抵达地
 * @param daysCount 天数、晚数或使用天数
 * @param resourceName 资源名称，例如酒店、景区、餐厅、购物店
 * @param supplierId 供应商ID
 * @param supplierName 供应商名称
 * @param driverName 司机信息
 * @param vehiclePlate 车牌号
 * @param trafficType 交通类型
 * @param vehicleType 车型
 * @param mealType 用餐时间或餐型
 * @param fundIncluded 基金是否包含
 * @param confirmed 是否已确认
 * @param confirmationNo 确认号
 * @param guideId 导游ID
 * @param guideName 导游名称
 * @param responsibleEmployeeId 责任房调、车调或计调员工ID
 * @param responsibleEmployeeName 责任员工名称
 * @param orderScope 订单归属说明
 * @param totalAmount 合计成本或总金额
 * @param cashAmount 现结金额
 * @param creditAmount 挂账金额
 * @param prepaidAmount 预付款金额
 * @param saleAmount 自费或购物收入合计
 * @param costAmount 成本合计
 * @param guideCommissionAmount 导游提成金额
 * @param companyRebateAmount 公司返佣金额
 * @param headFeeAmount 人头费金额
 * @param consumptionAmount 消费金额
 * @param peopleCount 人数
 * @param noGuideReport 是否无需导游报账
 * @param priceLines 价格明细
 */
public record SalesProductArrangementItemRequest(
        @NotBlank(message = "团队安排类型不能为空")
        @Pattern(
                regexp = "traffic|hotel|vehicle|scenic|meal|other|optional|shopping|ground_agent|extra_fee",
                message = "团队安排类型不合法"
        )
        String arrangementType,
        @NotBlank(message = "安排项目名称不能为空") @Size(max = 160) String itemName,
        String arrangementContent,
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,
        @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal unitPrice,
        @Size(max = 40) String unitName,
        @Pattern(regexp = "cash|credit", message = "结算类型不合法") String settlementType,
        String remark,
        @Pattern(regexp = "group_order_average|multi_order_average", message = "费用归属模式不合法") String allocationMode,
        @Size(max = 40) String scheduleStartDay,
        @Size(max = 40) String scheduleEndDay,
        @Size(max = 120) String departurePlace,
        @Size(max = 120) String arrivalPlace,
        Integer daysCount,
        @Size(max = 200) String resourceName,
        Long supplierId,
        @Size(max = 200) String supplierName,
        @Size(max = 100) String driverName,
        @Size(max = 40) String vehiclePlate,
        @Size(max = 40) String trafficType,
        @Size(max = 40) String vehicleType,
        @Size(max = 40) String mealType,
        @Size(max = 40) String fundIncluded,
        Boolean confirmed,
        @Size(max = 100) String confirmationNo,
        Long guideId,
        @Size(max = 100) String guideName,
        Long responsibleEmployeeId,
        @Size(max = 100) String responsibleEmployeeName,
        @Size(max = 120) String orderScope,
        @DecimalMin(value = "0", message = "合计金额不能小于0") BigDecimal totalAmount,
        @DecimalMin(value = "0", message = "现结金额不能小于0") BigDecimal cashAmount,
        @DecimalMin(value = "0", message = "挂账金额不能小于0") BigDecimal creditAmount,
        @DecimalMin(value = "0", message = "预付款不能小于0") BigDecimal prepaidAmount,
        @DecimalMin(value = "0", message = "收入金额不能小于0") BigDecimal saleAmount,
        @DecimalMin(value = "0", message = "成本金额不能小于0") BigDecimal costAmount,
        @DecimalMin(value = "0", message = "导游提成不能小于0") BigDecimal guideCommissionAmount,
        @DecimalMin(value = "0", message = "公司返佣不能小于0") BigDecimal companyRebateAmount,
        @DecimalMin(value = "0", message = "人头费不能小于0") BigDecimal headFeeAmount,
        @DecimalMin(value = "0", message = "消费金额不能小于0") BigDecimal consumptionAmount,
        @DecimalMin(value = "0", message = "人数不能小于0") BigDecimal peopleCount,
        Boolean noGuideReport,
        List<SalesProductArrangementPriceLineRequest> priceLines
) {}
