package com.mtravel.platform.sales.team.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;

/**
 * 团队列表查询汇总实体，对应 sales_team_list_summaries 表。
 *
 * <p>该表服务团队管理列表和拼团目标团选择列表，提前冗余团队、产品快照、订单、导游和安排状态摘要，
 * 避免列表页在大数据量下实时联查多张业务明细表。</p>
 */
@TableName("sales_team_list_summaries")
public class SalesTeamListSummaryEntity extends TenantSoftDeleteEntity {

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团队编号。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队展示名称。 */
    @TableField("team_name")
    private String teamName;

    /** 团队类型。 */
    @TableField("team_type")
    private String teamType;

    /** 团队状态。 */
    @TableField("status")
    private String status;

    /** 出团日期。 */
    @TableField("departure_date")
    private LocalDate departureDate;

    /** 结束日期。 */
    @TableField("end_date")
    private LocalDate endDate;

    /** 出发地展示文本。 */
    @TableField("departure_place")
    private String departurePlace;

    /** 行程天数。 */
    @TableField("travel_days")
    private Integer travelDays;

    /** 业务类型。 */
    @TableField("business_type")
    private String businessType;

    /** 归属部门名称。 */
    @TableField("department_name")
    private String departmentName;

    /** 操作计调姓名。 */
    @TableField("operator_employee_name")
    private String operatorEmployeeName;

    /** 客户单位摘要。 */
    @TableField("customer_summary")
    private String customerSummary;

    /** 业务员摘要。 */
    @TableField("salesperson_summary")
    private String salespersonSummary;

    /** 导游摘要。 */
    @TableField("guide_summary")
    private String guideSummary;

    /** 订单状态摘要。 */
    @TableField("order_status_summary")
    private String orderStatusSummary;

    /** 总位数。 */
    @TableField("total_seats")
    private Integer totalSeats;

    /** 实收人数。 */
    @TableField("used_seats")
    private Integer usedSeats;

    /** 剩余人数。 */
    @TableField("remaining_seats")
    private Integer remainingSeats;

    @TableField("guide_plan")
    private String guidePlan;

    @TableField("traffic_plan")
    private String trafficPlan;

    @TableField("hotel_plan")
    private String hotelPlan;

    @TableField("vehicle_plan")
    private String vehiclePlan;

    @TableField("scenic_plan")
    private String scenicPlan;

    @TableField("meal_plan")
    private String mealPlan;

    @TableField("other_plan")
    private String otherPlan;

    @TableField("optional_plan")
    private String optionalPlan;

    @TableField("shopping_plan")
    private String shoppingPlan;

    @TableField("ground_agent_plan")
    private String groundAgentPlan;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getDeparturePlace() { return departurePlace; }
    public void setDeparturePlace(String departurePlace) { this.departurePlace = departurePlace; }
    public Integer getTravelDays() { return travelDays; }
    public void setTravelDays(Integer travelDays) { this.travelDays = travelDays; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getOperatorEmployeeName() { return operatorEmployeeName; }
    public void setOperatorEmployeeName(String operatorEmployeeName) { this.operatorEmployeeName = operatorEmployeeName; }
    public String getCustomerSummary() { return customerSummary; }
    public void setCustomerSummary(String customerSummary) { this.customerSummary = customerSummary; }
    public String getSalespersonSummary() { return salespersonSummary; }
    public void setSalespersonSummary(String salespersonSummary) { this.salespersonSummary = salespersonSummary; }
    public String getGuideSummary() { return guideSummary; }
    public void setGuideSummary(String guideSummary) { this.guideSummary = guideSummary; }
    public String getOrderStatusSummary() { return orderStatusSummary; }
    public void setOrderStatusSummary(String orderStatusSummary) { this.orderStatusSummary = orderStatusSummary; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public Integer getUsedSeats() { return usedSeats; }
    public void setUsedSeats(Integer usedSeats) { this.usedSeats = usedSeats; }
    public Integer getRemainingSeats() { return remainingSeats; }
    public void setRemainingSeats(Integer remainingSeats) { this.remainingSeats = remainingSeats; }
    public String getGuidePlan() { return guidePlan; }
    public void setGuidePlan(String guidePlan) { this.guidePlan = guidePlan; }
    public String getTrafficPlan() { return trafficPlan; }
    public void setTrafficPlan(String trafficPlan) { this.trafficPlan = trafficPlan; }
    public String getHotelPlan() { return hotelPlan; }
    public void setHotelPlan(String hotelPlan) { this.hotelPlan = hotelPlan; }
    public String getVehiclePlan() { return vehiclePlan; }
    public void setVehiclePlan(String vehiclePlan) { this.vehiclePlan = vehiclePlan; }
    public String getScenicPlan() { return scenicPlan; }
    public void setScenicPlan(String scenicPlan) { this.scenicPlan = scenicPlan; }
    public String getMealPlan() { return mealPlan; }
    public void setMealPlan(String mealPlan) { this.mealPlan = mealPlan; }
    public String getOtherPlan() { return otherPlan; }
    public void setOtherPlan(String otherPlan) { this.otherPlan = otherPlan; }
    public String getOptionalPlan() { return optionalPlan; }
    public void setOptionalPlan(String optionalPlan) { this.optionalPlan = optionalPlan; }
    public String getShoppingPlan() { return shoppingPlan; }
    public void setShoppingPlan(String shoppingPlan) { this.shoppingPlan = shoppingPlan; }
    public String getGroundAgentPlan() { return groundAgentPlan; }
    public void setGroundAgentPlan(String groundAgentPlan) { this.groundAgentPlan = groundAgentPlan; }
}
