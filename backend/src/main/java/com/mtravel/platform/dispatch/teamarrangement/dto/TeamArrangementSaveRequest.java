package com.mtravel.platform.dispatch.teamarrangement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 正式团队安排保存请求。
 *
 * <p>该请求是团队实际成本，不是产品模板。订单归属字段用于团队公共成本、单订单成本和多订单均摊拆分。</p>
 *
 * @param arrangementId 安排 ID，新增为空，修改时传入
 * @param arrangementType 资源类型
 * @param itemName 安排名称
 * @param arrangementContent 安排摘要
 * @param allocationMode 费用归属模式
 * @param selectedOrderIds 关联订单 ID 列表；为空表示不关联订单
 * @param multiOrderSplitMode 多订单均摊方式
 * @param scheduleStartDay 使用日期或开始日期
 * @param scheduleEndDay 结束日期
 * @param departurePlace 出发地
 * @param arrivalPlace 抵达地
 * @param daysCount 天数、晚数或使用天数
 * @param resourceName 资源名称
 * @param supplierId 供应商 ID
 * @param supplierName 供应商名称
 * @param trafficType 交通类型
 * @param vehicleType 车型
 * @param driverName 司机
 * @param vehiclePlate 车牌
 * @param responsibleEmployeeId 责任员工 ID
 * @param responsibleEmployeeName 责任员工名称
 * @param totalAmount 合计金额
 * @param cashAmount 现结金额
 * @param creditAmount 挂账金额
 * @param prepaidAmount 预付款金额
 * @param saleAmount 收入金额
 * @param costAmount 成本金额
 * @param guideCommissionAmount 导游提成
 * @param companyRebateAmount 公司返佣
 * @param headFeeAmount 人头费
 * @param consumptionAmount 消费金额
 * @param peopleCount 人数
 * @param noGuideReport 是否无需导游报账
 * @param priceLines 价格明细
 * @param remark 备注
 * @param settlementType 默认结算类型，cash 现结，credit 挂账
 * @param mealType 餐型、用餐时间或酒店早餐类型
 * @param fundIncluded 基金或附加项目是否包含
 * @param confirmed 资源安排是否已确认
 * @param confirmationNo 资源确认号或供应商确认编号
 * @param guideId 关联导游 ID
 * @param guideName 关联导游姓名快照
 */
public record TeamArrangementSaveRequest(
        Long arrangementId,
        @NotBlank(message = "团队安排类型不能为空")
        @Pattern(
                regexp = "traffic|hotel|vehicle|scenic|meal|other|optional|shopping|ground_agent|extra_fee",
                message = "团队安排类型不合法"
        )
        String arrangementType,
        @NotBlank(message = "安排名称不能为空") @Size(max = 160, message = "安排名称不能超过160字") String itemName,
        String arrangementContent,
        @Pattern(regexp = "group_order_average|multi_order_average", message = "费用归属模式不合法") String allocationMode,
        List<Long> selectedOrderIds,
        @Pattern(regexp = "by_order|by_people", message = "多订单均摊方式不合法") String multiOrderSplitMode,
        @Size(max = 40, message = "日期字段不能超过40字") String scheduleStartDay,
        @Size(max = 40, message = "日期字段不能超过40字") String scheduleEndDay,
        @Size(max = 120, message = "出发地不能超过120字") String departurePlace,
        @Size(max = 120, message = "抵达地不能超过120字") String arrivalPlace,
        Integer daysCount,
        @Size(max = 200, message = "资源名称不能超过200字") String resourceName,
        Long supplierId,
        @Size(max = 200, message = "供应商名称不能超过200字") String supplierName,
        @Size(max = 40, message = "交通类型不能超过40字") String trafficType,
        @Size(max = 40, message = "车型不能超过40字") String vehicleType,
        @Size(max = 100, message = "司机不能超过100字") String driverName,
        @Size(max = 40, message = "车牌不能超过40字") String vehiclePlate,
        Long responsibleEmployeeId,
        @Size(max = 100, message = "责任员工不能超过100字") String responsibleEmployeeName,
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
        List<@Valid TeamArrangementPriceLineRequest> priceLines,
        String remark,
        @Pattern(regexp = "cash|credit", message = "结算类型不合法") String settlementType,
        @Size(max = 40, message = "餐型不能超过40字") String mealType,
        @Size(max = 40, message = "基金信息不能超过40字") String fundIncluded,
        Boolean confirmed,
        @Size(max = 100, message = "确认号不能超过100字") String confirmationNo,
        Long guideId,
        @Size(max = 100, message = "导游姓名不能超过100字") String guideName
) {
    /**
     * 兼容已有单元测试和内部调用的旧参数构造器。
     */
    public TeamArrangementSaveRequest(
            Long arrangementId,
            String arrangementType,
            String itemName,
            String arrangementContent,
            String allocationMode,
            List<Long> selectedOrderIds,
            String multiOrderSplitMode,
            String scheduleStartDay,
            String scheduleEndDay,
            String departurePlace,
            String arrivalPlace,
            Integer daysCount,
            String resourceName,
            Long supplierId,
            String supplierName,
            String trafficType,
            String vehicleType,
            String driverName,
            String vehiclePlate,
            Long responsibleEmployeeId,
            String responsibleEmployeeName,
            BigDecimal totalAmount,
            BigDecimal cashAmount,
            BigDecimal creditAmount,
            BigDecimal prepaidAmount,
            BigDecimal saleAmount,
            BigDecimal costAmount,
            BigDecimal guideCommissionAmount,
            BigDecimal companyRebateAmount,
            BigDecimal headFeeAmount,
            BigDecimal consumptionAmount,
            BigDecimal peopleCount,
            Boolean noGuideReport,
            List<@Valid TeamArrangementPriceLineRequest> priceLines,
            String remark
    ) {
        this(
                arrangementId,
                arrangementType,
                itemName,
                arrangementContent,
                allocationMode,
                selectedOrderIds,
                multiOrderSplitMode,
                scheduleStartDay,
                scheduleEndDay,
                departurePlace,
                arrivalPlace,
                daysCount,
                resourceName,
                supplierId,
                supplierName,
                trafficType,
                vehicleType,
                driverName,
                vehiclePlate,
                responsibleEmployeeId,
                responsibleEmployeeName,
                totalAmount,
                cashAmount,
                creditAmount,
                prepaidAmount,
                saleAmount,
                costAmount,
                guideCommissionAmount,
                companyRebateAmount,
                headFeeAmount,
                consumptionAmount,
                peopleCount,
                noGuideReport,
                priceLines,
                remark,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
