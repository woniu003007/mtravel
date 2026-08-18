package com.mtravel.platform.sales.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * @param vehicleQuoteSnapshot 用车报价测算快照，仅用车安排使用
 * @param vehicleInquiryRecords 用车询价记录，仅用车安排使用
 */
@Schema(name = "SalesProductArrangementItemRequest", description = "销售产品团队安排参数保存请求")
public record SalesProductArrangementItemRequest(
        @Schema(
                description = "团队安排类型：traffic 交通，hotel 住宿，vehicle 用车，scenic 景区，meal 用餐，other 其他，optional 自费，shopping 购物，ground_agent 地接社，extra_fee 杂费",
                example = "hotel",
                allowableValues = {"traffic", "hotel", "vehicle", "scenic", "meal", "other", "optional", "shopping", "ground_agent", "extra_fee"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "团队安排类型不能为空")
        @Pattern(
                regexp = "traffic|hotel|vehicle|scenic|meal|other|optional|shopping|ground_agent|extra_fee",
                message = "团队安排类型不合法"
        )
        String arrangementType,

        @Schema(description = "安排项目名称", example = "南京住宿", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "安排项目名称不能为空") @Size(max = 160)
        String itemName,

        @Schema(description = "安排内容或默认说明", example = "南京市区舒适型酒店 1 晚")
        String arrangementContent,

        @Schema(description = "默认数量", example = "1", minimum = "0")
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,

        @Schema(description = "默认单价或费用参考", example = "240.00", minimum = "0")
        @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal unitPrice,

        @Schema(description = "计量单位", example = "晚")
        @Size(max = 40) String unitName,

        @Schema(description = "结算类型：cash 现结，credit 挂账", example = "credit", allowableValues = {"cash", "credit"})
        @Pattern(regexp = "cash|credit", message = "结算类型不合法") String settlementType,

        @Schema(description = "备注", example = "最终酒店以计调确认为准")
        String remark,

        @Schema(description = "费用归属模式：group_order_average 全团/订单均摊，multi_order_average 多订单均摊成本", example = "group_order_average", allowableValues = {"group_order_average", "multi_order_average"})
        @Pattern(regexp = "group_order_average|multi_order_average", message = "费用归属模式不合法") String allocationMode,

        @Schema(description = "开始或使用日期/天数标记", example = "D1")
        @Size(max = 40) String scheduleStartDay,

        @Schema(description = "结束日期/天数标记", example = "D2")
        @Size(max = 40) String scheduleEndDay,

        @Schema(description = "出发地", example = "南京")
        @Size(max = 120) String departurePlace,

        @Schema(description = "抵达地", example = "苏州")
        @Size(max = 120) String arrivalPlace,

        @Schema(description = "天数、晚数或使用天数", example = "1")
        Integer daysCount,

        @Schema(description = "资源名称，例如酒店、景区、餐厅、购物店", example = "南京市区酒店")
        @Size(max = 200) String resourceName,

        @Schema(description = "供应商 ID", example = "1001")
        Long supplierId,

        @Schema(description = "供应商名称", example = "南京某酒店供应商")
        @Size(max = 200) String supplierName,

        @Schema(description = "司机信息", example = "张师傅 13800000000")
        @Size(max = 100) String driverName,

        @Schema(description = "车牌号", example = "苏A12345")
        @Size(max = 40) String vehiclePlate,

        @Schema(description = "交通类型", example = "旅游大巴")
        @Size(max = 40) String trafficType,

        @Schema(description = "车型", example = "33座")
        @Size(max = 40) String vehicleType,

        @Schema(description = "用餐时间或餐型", example = "晚餐")
        @Size(max = 40) String mealType,

        @Schema(description = "基金是否包含", example = "included")
        @Size(max = 40) String fundIncluded,

        @Schema(description = "是否已确认", example = "false")
        Boolean confirmed,

        @Schema(description = "确认号", example = "HT20260720001")
        @Size(max = 100) String confirmationNo,

        @Schema(description = "导游 ID", example = "2001")
        Long guideId,

        @Schema(description = "导游名称", example = "王导")
        @Size(max = 100) String guideName,

        @Schema(description = "责任房调、车调或计调员工 ID", example = "3001")
        Long responsibleEmployeeId,

        @Schema(description = "责任员工名称", example = "李计调")
        @Size(max = 100) String responsibleEmployeeName,

        @Schema(description = "订单归属说明", example = "全团均摊")
        @Size(max = 120) String orderScope,

        @Schema(description = "合计成本或总金额", example = "7200.00", minimum = "0")
        @DecimalMin(value = "0", message = "合计金额不能小于0") BigDecimal totalAmount,

        @Schema(description = "现结金额", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "现结金额不能小于0") BigDecimal cashAmount,

        @Schema(description = "挂账金额", example = "7200.00", minimum = "0")
        @DecimalMin(value = "0", message = "挂账金额不能小于0") BigDecimal creditAmount,

        @Schema(description = "预付款金额", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "预付款不能小于0") BigDecimal prepaidAmount,

        @Schema(description = "自费或购物收入合计", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "收入金额不能小于0") BigDecimal saleAmount,

        @Schema(description = "成本合计", example = "7200.00", minimum = "0")
        @DecimalMin(value = "0", message = "成本金额不能小于0") BigDecimal costAmount,

        @Schema(description = "导游提成金额", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "导游提成不能小于0") BigDecimal guideCommissionAmount,

        @Schema(description = "公司返佣金额", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "公司返佣不能小于0") BigDecimal companyRebateAmount,

        @Schema(description = "人头费金额", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "人头费不能小于0") BigDecimal headFeeAmount,

        @Schema(description = "消费金额", example = "0.00", minimum = "0")
        @DecimalMin(value = "0", message = "消费金额不能小于0") BigDecimal consumptionAmount,

        @Schema(description = "人数", example = "30", minimum = "0")
        @DecimalMin(value = "0", message = "人数不能小于0") BigDecimal peopleCount,

        @Schema(description = "是否无需导游报账", example = "false")
        Boolean noGuideReport,

        @Schema(description = "价格明细")
        List<SalesProductArrangementPriceLineRequest> priceLines,

        @Schema(description = "用车报价测算快照，仅用车安排使用")
        SalesProductVehicleQuoteSnapshotRequest vehicleQuoteSnapshot,

        @Schema(description = "用车询价记录，仅用车安排使用")
        List<SalesProductVehicleInquiryRequest> vehicleInquiryRecords
) {}
