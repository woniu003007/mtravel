package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售产品团队安排参数实体，对应 sales_product_arrangement_items 表。
 *
 * <p>该表保存产品模板阶段的大交通、住宿、用车、景区等默认安排和费用参考，不代表真实排团履约。</p>
 */
@TableName("sales_product_arrangement_items")
public class SalesProductArrangementItemEntity extends TenantSoftDeleteEntity {

    /** 所属产品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 安排类型，例如 hotel、vehicle、scenic。 */
    @TableField("arrangement_type")
    private String arrangementType;

    /** 安排项目名称。 */
    @TableField("item_name")
    private String itemName;

    /** 安排内容或默认说明。 */
    @TableField("arrangement_content")
    private String arrangementContent;

    /** 默认数量。 */
    @TableField("quantity")
    private BigDecimal quantity;

    /** 默认单价或费用参考。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 计量单位。 */
    @TableField("unit_name")
    private String unitName;

    /** 结算类型。cash 现结，credit 挂账。 */
    @TableField("settlement_type")
    private String settlementType;

    /** 费用归属模式。group_order_average 表示全团或订单均摊，multi_order_average 表示多订单均摊成本。 */
    @TableField("allocation_mode")
    private String allocationMode;

    /** 使用开始日期或行程第几天。 */
    @TableField("schedule_start_day")
    private String scheduleStartDay;

    /** 使用结束日期或退房日期。 */
    @TableField("schedule_end_day")
    private String scheduleEndDay;

    /** 出发地，主要用于大交通。 */
    @TableField("departure_place")
    private String departurePlace;

    /** 目的地，主要用于大交通。 */
    @TableField("arrival_place")
    private String arrivalPlace;

    /** 天数、晚数或使用天数。 */
    @TableField("days_count")
    private Integer daysCount;

    /** 资源名称，例如酒店、景区、餐厅或购物店。 */
    @TableField("resource_name")
    private String resourceName;

    /** 供应商 ID，关联采购资源供应商。 */
    @TableField("supplier_id")
    private Long supplierId;

    /** 供应商名称快照，便于产品模板历史回显。 */
    @TableField("supplier_name")
    private String supplierName;

    /** 司机姓名或联系方式，主要用于用车安排。 */
    @TableField("driver_name")
    private String driverName;

    /** 车牌号，主要用于用车安排。 */
    @TableField("vehicle_plate")
    private String vehiclePlate;

    /** 交通类型，例如飞机、高铁、火车。 */
    @TableField("traffic_type")
    private String trafficType;

    /** 车型，例如 7 座、39 座、54 座。 */
    @TableField("vehicle_type")
    private String vehicleType;

    /** 用餐时间或餐型，例如早餐、中餐、晚餐。 */
    @TableField("meal_type")
    private String mealType;

    /** 酒店基金是否包含。 */
    @TableField("fund_included")
    private String fundIncluded;

    /** 是否已确认。 */
    @TableField("confirmed")
    private Boolean confirmed;

    /** 确认号。 */
    @TableField("confirmation_no")
    private String confirmationNo;

    /** 导游 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 责任员工 ID，例如房调、车调或计调。 */
    @TableField("responsible_employee_id")
    private Long responsibleEmployeeId;

    /** 责任员工名称快照。 */
    @TableField("responsible_employee_name")
    private String responsibleEmployeeName;

    /** 订单归属说明。产品模板阶段默认不关联正式订单。 */
    @TableField("order_scope")
    private String orderScope;

    /** 合计成本或总金额。 */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /** 现结金额。 */
    @TableField("cash_amount")
    private BigDecimal cashAmount;

    /** 挂账金额。 */
    @TableField("credit_amount")
    private BigDecimal creditAmount;

    /** 预付款金额。 */
    @TableField("prepaid_amount")
    private BigDecimal prepaidAmount;

    /** 收入合计，主要用于自费项目。 */
    @TableField("sale_amount")
    private BigDecimal saleAmount;

    /** 成本合计。 */
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /** 导游提成金额。 */
    @TableField("guide_commission_amount")
    private BigDecimal guideCommissionAmount;

    /** 公司返佣金额。 */
    @TableField("company_rebate_amount")
    private BigDecimal companyRebateAmount;

    /** 人头费金额。 */
    @TableField("head_fee_amount")
    private BigDecimal headFeeAmount;

    /** 消费金额。 */
    @TableField("consumption_amount")
    private BigDecimal consumptionAmount;

    /** 人数。 */
    @TableField("people_count")
    private BigDecimal peopleCount;

    /** 是否无需导游报账，同步计调审核数据。 */
    @TableField("no_guide_report")
    private Boolean noGuideReport;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(String arrangementType) {
        this.arrangementType = arrangementType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getArrangementContent() {
        return arrangementContent;
    }

    public void setArrangementContent(String arrangementContent) {
        this.arrangementContent = arrangementContent;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(String settlementType) {
        this.settlementType = settlementType;
    }

    public String getAllocationMode() { return allocationMode; }
    public void setAllocationMode(String allocationMode) { this.allocationMode = allocationMode; }
    public String getScheduleStartDay() { return scheduleStartDay; }
    public void setScheduleStartDay(String scheduleStartDay) { this.scheduleStartDay = scheduleStartDay; }
    public String getScheduleEndDay() { return scheduleEndDay; }
    public void setScheduleEndDay(String scheduleEndDay) { this.scheduleEndDay = scheduleEndDay; }
    public String getDeparturePlace() { return departurePlace; }
    public void setDeparturePlace(String departurePlace) { this.departurePlace = departurePlace; }
    public String getArrivalPlace() { return arrivalPlace; }
    public void setArrivalPlace(String arrivalPlace) { this.arrivalPlace = arrivalPlace; }
    public Integer getDaysCount() { return daysCount; }
    public void setDaysCount(Integer daysCount) { this.daysCount = daysCount; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getTrafficType() { return trafficType; }
    public void setTrafficType(String trafficType) { this.trafficType = trafficType; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getFundIncluded() { return fundIncluded; }
    public void setFundIncluded(String fundIncluded) { this.fundIncluded = fundIncluded; }
    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
    public String getConfirmationNo() { return confirmationNo; }
    public void setConfirmationNo(String confirmationNo) { this.confirmationNo = confirmationNo; }
    public Long getGuideId() { return guideId; }
    public void setGuideId(Long guideId) { this.guideId = guideId; }
    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }
    public Long getResponsibleEmployeeId() { return responsibleEmployeeId; }
    public void setResponsibleEmployeeId(Long responsibleEmployeeId) { this.responsibleEmployeeId = responsibleEmployeeId; }
    public String getResponsibleEmployeeName() { return responsibleEmployeeName; }
    public void setResponsibleEmployeeName(String responsibleEmployeeName) { this.responsibleEmployeeName = responsibleEmployeeName; }
    public String getOrderScope() { return orderScope; }
    public void setOrderScope(String orderScope) { this.orderScope = orderScope; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getCashAmount() { return cashAmount; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
    public BigDecimal getPrepaidAmount() { return prepaidAmount; }
    public void setPrepaidAmount(BigDecimal prepaidAmount) { this.prepaidAmount = prepaidAmount; }
    public BigDecimal getSaleAmount() { return saleAmount; }
    public void setSaleAmount(BigDecimal saleAmount) { this.saleAmount = saleAmount; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    public BigDecimal getGuideCommissionAmount() { return guideCommissionAmount; }
    public void setGuideCommissionAmount(BigDecimal guideCommissionAmount) { this.guideCommissionAmount = guideCommissionAmount; }
    public BigDecimal getCompanyRebateAmount() { return companyRebateAmount; }
    public void setCompanyRebateAmount(BigDecimal companyRebateAmount) { this.companyRebateAmount = companyRebateAmount; }
    public BigDecimal getHeadFeeAmount() { return headFeeAmount; }
    public void setHeadFeeAmount(BigDecimal headFeeAmount) { this.headFeeAmount = headFeeAmount; }
    public BigDecimal getConsumptionAmount() { return consumptionAmount; }
    public void setConsumptionAmount(BigDecimal consumptionAmount) { this.consumptionAmount = consumptionAmount; }
    public BigDecimal getPeopleCount() { return peopleCount; }
    public void setPeopleCount(BigDecimal peopleCount) { this.peopleCount = peopleCount; }
    public Boolean getNoGuideReport() { return noGuideReport; }
    public void setNoGuideReport(Boolean noGuideReport) { this.noGuideReport = noGuideReport; }
}
