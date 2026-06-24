package com.mtravel.platform.sales.team.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售团队主实体，对应 sales_teams 表。
 *
 * <p>团期管理生成的是正式团队记录，不是产品模板字段。团队级字段只保存团号、发团日期、
 * 操作计调、状态、座位和房差，客户类型价格放在 sales_team_prices。</p>
 */
@TableName("sales_teams")
public class SalesTeamEntity extends TenantSoftDeleteEntity {

    /** 所属销售产品模板 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 团队编号。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队类型，团期管理默认 sanpin。 */
    @TableField("team_type")
    private String teamType;

    /** 团队业务类型快照，允许正式团队覆盖产品模板业务类型。 */
    @TableField("business_type")
    private String businessType;

    /** 发团日期。 */
    @TableField("departure_date")
    private LocalDate departureDate;

    /** 团队归属部门 ID。 */
    @TableField("department_id")
    private Long departmentId;

    /** 团队归属部门名称快照。 */
    @TableField("department_name")
    private String departmentName;

    /** 操作计调员工 ID。 */
    @TableField("operator_employee_id")
    private Long operatorEmployeeId;

    /** 操作计调员工姓名快照。 */
    @TableField("operator_employee_name")
    private String operatorEmployeeName;

    /** 全陪员工 ID。 */
    @TableField("escort_employee_id")
    private Long escortEmployeeId;

    /** 全陪员工姓名快照。 */
    @TableField("escort_employee_name")
    private String escortEmployeeName;

    /** 团队状态：normal、stopped、cancelled。 */
    @TableField("status")
    private String status;

    /** 总位数。 */
    @TableField("total_seats")
    private Integer totalSeats;

    /** 已占用位数。 */
    @TableField("used_seats")
    private Integer usedSeats;

    /** 剩余位数。 */
    @TableField("remaining_seats")
    private Integer remainingSeats;

    /** 单人房差价格。 */
    @TableField("single_room_difference")
    private BigDecimal singleRoomDifference;

    /** 出团前截止收客天数。 */
    @TableField("close_days_before")
    private Integer closeDaysBefore;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getTeamNo() {
        return teamNo;
    }

    public void setTeamNo(String teamNo) {
        this.teamNo = teamNo;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getOperatorEmployeeId() {
        return operatorEmployeeId;
    }

    public void setOperatorEmployeeId(Long operatorEmployeeId) {
        this.operatorEmployeeId = operatorEmployeeId;
    }

    public String getOperatorEmployeeName() {
        return operatorEmployeeName;
    }

    public void setOperatorEmployeeName(String operatorEmployeeName) {
        this.operatorEmployeeName = operatorEmployeeName;
    }

    public Long getEscortEmployeeId() {
        return escortEmployeeId;
    }

    public void setEscortEmployeeId(Long escortEmployeeId) {
        this.escortEmployeeId = escortEmployeeId;
    }

    public String getEscortEmployeeName() {
        return escortEmployeeName;
    }

    public void setEscortEmployeeName(String escortEmployeeName) {
        this.escortEmployeeName = escortEmployeeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getUsedSeats() {
        return usedSeats;
    }

    public void setUsedSeats(Integer usedSeats) {
        this.usedSeats = usedSeats;
    }

    public Integer getRemainingSeats() {
        return remainingSeats;
    }

    public void setRemainingSeats(Integer remainingSeats) {
        this.remainingSeats = remainingSeats;
    }

    public BigDecimal getSingleRoomDifference() {
        return singleRoomDifference;
    }

    public void setSingleRoomDifference(BigDecimal singleRoomDifference) {
        this.singleRoomDifference = singleRoomDifference;
    }

    public Integer getCloseDaysBefore() {
        return closeDaysBefore;
    }

    public void setCloseDaysBefore(Integer closeDaysBefore) {
        this.closeDaysBefore = closeDaysBefore;
    }
}
