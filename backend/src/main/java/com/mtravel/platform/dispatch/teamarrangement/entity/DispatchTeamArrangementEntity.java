package com.mtravel.platform.dispatch.teamarrangement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 正式团队安排成本主表实体。
 *
 * <p>本实体保存团队实际执行阶段产生的资源安排和成本，是后续应付、导游报账、计调审核、
 * 财务审核、团队毛利和资源采购统计的源头数据。</p>
 */
@TableName("dispatch_team_arrangements")
public class DispatchTeamArrangementEntity extends TenantSoftDeleteEntity {

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团队编号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队类型快照。 */
    @TableField("team_type")
    private String teamType;

    /** 团队业务类型快照。 */
    @TableField("business_type")
    private String businessType;

    /** 部门 ID 快照。 */
    @TableField("department_id")
    private Long departmentId;

    /** 部门名称快照。 */
    @TableField("department_name")
    private String departmentName;

    /** 操作计调员工 ID 快照。 */
    @TableField("operator_employee_id")
    private Long operatorEmployeeId;

    /** 操作计调姓名快照。 */
    @TableField("operator_employee_name")
    private String operatorEmployeeName;

    /** 资源安排类型。 */
    @TableField("arrangement_type")
    private String arrangementType;

    /** 安排名称。 */
    @TableField("item_name")
    private String itemName;

    /** 安排摘要。 */
    @TableField("arrangement_content")
    private String arrangementContent;

    /** 成本归属模式。 */
    @TableField("allocation_mode")
    private String allocationMode;

    /** 多订单均摊方式。 */
    @TableField("split_mode")
    private String splitMode;

    /** 多订单拆分批次号。 */
    @TableField("split_batch_no")
    private String splitBatchNo;

    /** 开始或使用日期文本。 */
    @TableField("schedule_start_day")
    private String scheduleStartDay;

    /** 结束日期文本。 */
    @TableField("schedule_end_day")
    private String scheduleEndDay;

    /** 业务日期，用于财务和统计按日期查询。 */
    @TableField("business_date")
    private LocalDate businessDate;

    /** 出发地。 */
    @TableField("departure_place")
    private String departurePlace;

    /** 抵达地。 */
    @TableField("arrival_place")
    private String arrivalPlace;

    /** 天数、晚数或使用天数。 */
    @TableField("days_count")
    private Integer daysCount;

    /** 资源名称。 */
    @TableField("resource_name")
    private String resourceName;

    /** 供应商 ID。 */
    @TableField("supplier_id")
    private Long supplierId;

    /** 供应商名称快照。 */
    @TableField("supplier_name")
    private String supplierName;

    /** 交通类型。 */
    @TableField("traffic_type")
    private String trafficType;

    /** 车型。 */
    @TableField("vehicle_type")
    private String vehicleType;

    /** 司机。 */
    @TableField("driver_name")
    private String driverName;

    /** 车牌号。 */
    @TableField("vehicle_plate")
    private String vehiclePlate;

    /** 责任员工 ID。 */
    @TableField("responsible_employee_id")
    private Long responsibleEmployeeId;

    /** 责任员工名称。 */
    @TableField("responsible_employee_name")
    private String responsibleEmployeeName;

    /** 默认结算类型。cash 现结，credit 挂账。 */
    @TableField("settlement_type")
    private String settlementType;

    /** 餐型、用餐时间或酒店早餐类型。 */
    @TableField("meal_type")
    private String mealType;

    /** 基金或附加项目是否包含。 */
    @TableField("fund_included")
    private String fundIncluded;

    /** 资源安排是否已确认。 */
    @TableField("confirmed")
    private Boolean confirmed;

    /** 资源确认号或供应商确认编号。 */
    @TableField("confirmation_no")
    private String confirmationNo;

    /** 关联导游 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 关联导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 合计金额。 */
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

    /** 收入金额。 */
    @TableField("sale_amount")
    private BigDecimal saleAmount;

    /** 成本金额。 */
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

    /** 是否无需导游报账。 */
    @TableField("no_guide_report")
    private Boolean noGuideReport;

    /** 导游是否参与报账。 */
    @TableField("guide_involved")
    private Boolean guideInvolved;

    /** 成本阶段。 */
    @TableField("cost_stage")
    private String costStage;

    /** 导游报账状态。 */
    @TableField("guide_report_status")
    private String guideReportStatus;

    /** 计调审核状态。 */
    @TableField("operator_audit_status")
    private String operatorAuditStatus;

    /** 财务审核状态。 */
    @TableField("finance_audit_status")
    private String financeAuditStatus;

    /** 安排状态。 */
    @TableField("status")
    private String status;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getOperatorEmployeeId() { return operatorEmployeeId; }
    public void setOperatorEmployeeId(Long operatorEmployeeId) { this.operatorEmployeeId = operatorEmployeeId; }
    public String getOperatorEmployeeName() { return operatorEmployeeName; }
    public void setOperatorEmployeeName(String operatorEmployeeName) { this.operatorEmployeeName = operatorEmployeeName; }
    public String getArrangementType() { return arrangementType; }
    public void setArrangementType(String arrangementType) { this.arrangementType = arrangementType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getArrangementContent() { return arrangementContent; }
    public void setArrangementContent(String arrangementContent) { this.arrangementContent = arrangementContent; }
    public String getAllocationMode() { return allocationMode; }
    public void setAllocationMode(String allocationMode) { this.allocationMode = allocationMode; }
    public String getSplitMode() { return splitMode; }
    public void setSplitMode(String splitMode) { this.splitMode = splitMode; }
    public String getSplitBatchNo() { return splitBatchNo; }
    public void setSplitBatchNo(String splitBatchNo) { this.splitBatchNo = splitBatchNo; }
    public String getScheduleStartDay() { return scheduleStartDay; }
    public void setScheduleStartDay(String scheduleStartDay) { this.scheduleStartDay = scheduleStartDay; }
    public String getScheduleEndDay() { return scheduleEndDay; }
    public void setScheduleEndDay(String scheduleEndDay) { this.scheduleEndDay = scheduleEndDay; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
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
    public String getTrafficType() { return trafficType; }
    public void setTrafficType(String trafficType) { this.trafficType = trafficType; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public Long getResponsibleEmployeeId() { return responsibleEmployeeId; }
    public void setResponsibleEmployeeId(Long responsibleEmployeeId) { this.responsibleEmployeeId = responsibleEmployeeId; }
    public String getResponsibleEmployeeName() { return responsibleEmployeeName; }
    public void setResponsibleEmployeeName(String responsibleEmployeeName) { this.responsibleEmployeeName = responsibleEmployeeName; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
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
    public Boolean getGuideInvolved() { return guideInvolved; }
    public void setGuideInvolved(Boolean guideInvolved) { this.guideInvolved = guideInvolved; }
    public String getCostStage() { return costStage; }
    public void setCostStage(String costStage) { this.costStage = costStage; }
    public String getGuideReportStatus() { return guideReportStatus; }
    public void setGuideReportStatus(String guideReportStatus) { this.guideReportStatus = guideReportStatus; }
    public String getOperatorAuditStatus() { return operatorAuditStatus; }
    public void setOperatorAuditStatus(String operatorAuditStatus) { this.operatorAuditStatus = operatorAuditStatus; }
    public String getFinanceAuditStatus() { return financeAuditStatus; }
    public void setFinanceAuditStatus(String financeAuditStatus) { this.financeAuditStatus = financeAuditStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
