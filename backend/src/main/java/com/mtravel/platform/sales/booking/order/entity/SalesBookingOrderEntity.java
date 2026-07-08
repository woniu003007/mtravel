package com.mtravel.platform.sales.booking.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 销售收客订单实体，对应 sales_orders 表。
 *
 * <p>本实体承载老系统收客订单页中的订单信息、行程说明、导游相关、客户信息、酒店信息、
 * 附加说明和金额汇总。游客名单和价格明细分别由子表保存。</p>
 */
@TableName("sales_orders")
public class SalesBookingOrderEntity extends TenantSoftDeleteEntity {

    /** 所属销售团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 订单编号。 */
    @TableField("order_no")
    private String orderNo;

    /** 客户单位 ID。 */
    @TableField("customer_id")
    private Long customerId;

    /** 客户单位名称快照。 */
    @TableField("customer_name")
    private String customerName;

    /** 联系人姓名。 */
    @TableField("contact_name")
    private String contactName;

    /** 联系人电话。 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 客户方团队编号。 */
    @TableField("customer_team_no")
    private String customerTeamNo;

    /** 原始订单摘要，用于团队操作页追溯拼团、转团或来源订单。 */
    @TableField("original_order_info")
    private String originalOrderInfo;

    /** 订单角色：normal 普通订单，merge_source 拼团来源留痕订单，merge_child 拼团目标子订单。 */
    @TableField("order_role")
    private String orderRole;

    /** 订单管理页标记状态，用于销售人员快速筛选重点订单。 */
    @TableField("tagging")
    private Boolean tagging;

    /** 业务员员工 ID。 */
    @TableField("salesperson_employee_id")
    private Long salespersonEmployeeId;

    /** 业务员姓名快照。 */
    @TableField("salesperson_employee_name")
    private String salespersonEmployeeName;

    /** 收客计调员工 ID。 */
    @TableField("booking_operator_employee_id")
    private Long bookingOperatorEmployeeId;

    /** 收客计调姓名快照。 */
    @TableField("booking_operator_employee_name")
    private String bookingOperatorEmployeeName;

    /** 客源地省份。 */
    @TableField("source_province")
    private String sourceProvince;

    /** 客源地城市。 */
    @TableField("source_city")
    private String sourceCity;

    /** 客源地区县。 */
    @TableField("source_district")
    private String sourceDistrict;

    /** 行程说明。 */
    @TableField("travel_description")
    private String travelDescription;

    /** 接站或接机信息。 */
    @TableField("pickup_info")
    private String pickupInfo;

    /** 送站或送机信息。 */
    @TableField("dropoff_info")
    private String dropoffInfo;

    /** 接送备注。 */
    @TableField("pickup_remark")
    private String pickupRemark;

    /** 导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 导游联系电话快照。 */
    @TableField("guide_phone")
    private String guidePhone;

    /** 导游相关备注。 */
    @TableField("guide_remark")
    private String guideRemark;

    /** 酒店信息或住宿要求。 */
    @TableField("hotel_info")
    private String hotelInfo;

    /** 成人数量。 */
    @TableField("adult_count")
    private Integer adultCount;

    /** 儿童占床数量。 */
    @TableField("child_count")
    private Integer childCount;

    /** 儿童不占床数量。 */
    @TableField("child_no_bed_count")
    private Integer childNoBedCount;

    /** 老人数量。 */
    @TableField("senior_count")
    private Integer seniorCount;

    /** 全陪数量。 */
    @TableField("escort_count")
    private Integer escortCount;

    /** 订单总人数。 */
    @TableField("guest_count")
    private Integer guestCount;

    /** 订单应收金额。 */
    @TableField("receivable_amount")
    private BigDecimal receivableAmount;

    /** 订单已收金额。 */
    @TableField("received_amount")
    private BigDecimal receivedAmount;

    /** 订单余额。 */
    @TableField("balance_amount")
    private BigDecimal balanceAmount;

    /** 费用说明。 */
    @TableField("fee_remark")
    private String feeRemark;

    /** 确认说明。 */
    @TableField("confirm_remark")
    private String confirmRemark;

    /** 订单备注。 */
    @TableField("order_remark")
    private String orderRemark;

    /** 订单状态：pending、confirmed、cancelled。 */
    @TableField("status")
    private String status;

    /** 下单人或预订人名称。 */
    @TableField("booked_by")
    private String bookedBy;

    /** 下单时间。 */
    @TableField("booked_at")
    private OffsetDateTime bookedAt;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getCustomerTeamNo() {
        return customerTeamNo;
    }

    public void setCustomerTeamNo(String customerTeamNo) {
        this.customerTeamNo = customerTeamNo;
    }

    public String getOriginalOrderInfo() {
        return originalOrderInfo;
    }

    public void setOriginalOrderInfo(String originalOrderInfo) {
        this.originalOrderInfo = originalOrderInfo;
    }

    public String getOrderRole() {
        return orderRole;
    }

    public void setOrderRole(String orderRole) {
        this.orderRole = orderRole;
    }

    public Boolean getTagging() {
        return tagging;
    }

    public void setTagging(Boolean tagging) {
        this.tagging = tagging;
    }

    public Long getSalespersonEmployeeId() {
        return salespersonEmployeeId;
    }

    public void setSalespersonEmployeeId(Long salespersonEmployeeId) {
        this.salespersonEmployeeId = salespersonEmployeeId;
    }

    public String getSalespersonEmployeeName() {
        return salespersonEmployeeName;
    }

    public void setSalespersonEmployeeName(String salespersonEmployeeName) {
        this.salespersonEmployeeName = salespersonEmployeeName;
    }

    public Long getBookingOperatorEmployeeId() {
        return bookingOperatorEmployeeId;
    }

    public void setBookingOperatorEmployeeId(Long bookingOperatorEmployeeId) {
        this.bookingOperatorEmployeeId = bookingOperatorEmployeeId;
    }

    public String getBookingOperatorEmployeeName() {
        return bookingOperatorEmployeeName;
    }

    public void setBookingOperatorEmployeeName(String bookingOperatorEmployeeName) {
        this.bookingOperatorEmployeeName = bookingOperatorEmployeeName;
    }

    public String getSourceProvince() {
        return sourceProvince;
    }

    public void setSourceProvince(String sourceProvince) {
        this.sourceProvince = sourceProvince;
    }

    public String getSourceCity() {
        return sourceCity;
    }

    public void setSourceCity(String sourceCity) {
        this.sourceCity = sourceCity;
    }

    public String getSourceDistrict() {
        return sourceDistrict;
    }

    public void setSourceDistrict(String sourceDistrict) {
        this.sourceDistrict = sourceDistrict;
    }

    public String getTravelDescription() {
        return travelDescription;
    }

    public void setTravelDescription(String travelDescription) {
        this.travelDescription = travelDescription;
    }

    public String getPickupInfo() {
        return pickupInfo;
    }

    public void setPickupInfo(String pickupInfo) {
        this.pickupInfo = pickupInfo;
    }

    public String getDropoffInfo() {
        return dropoffInfo;
    }

    public void setDropoffInfo(String dropoffInfo) {
        this.dropoffInfo = dropoffInfo;
    }

    public String getPickupRemark() {
        return pickupRemark;
    }

    public void setPickupRemark(String pickupRemark) {
        this.pickupRemark = pickupRemark;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public String getGuidePhone() {
        return guidePhone;
    }

    public void setGuidePhone(String guidePhone) {
        this.guidePhone = guidePhone;
    }

    public String getGuideRemark() {
        return guideRemark;
    }

    public void setGuideRemark(String guideRemark) {
        this.guideRemark = guideRemark;
    }

    public String getHotelInfo() {
        return hotelInfo;
    }

    public void setHotelInfo(String hotelInfo) {
        this.hotelInfo = hotelInfo;
    }

    public Integer getAdultCount() {
        return adultCount;
    }

    public void setAdultCount(Integer adultCount) {
        this.adultCount = adultCount;
    }

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }

    public Integer getChildNoBedCount() {
        return childNoBedCount;
    }

    public void setChildNoBedCount(Integer childNoBedCount) {
        this.childNoBedCount = childNoBedCount;
    }

    public Integer getSeniorCount() {
        return seniorCount;
    }

    public void setSeniorCount(Integer seniorCount) {
        this.seniorCount = seniorCount;
    }

    public Integer getEscortCount() {
        return escortCount;
    }

    public void setEscortCount(Integer escortCount) {
        this.escortCount = escortCount;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public BigDecimal getReceivableAmount() {
        return receivableAmount;
    }

    public void setReceivableAmount(BigDecimal receivableAmount) {
        this.receivableAmount = receivableAmount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public String getFeeRemark() {
        return feeRemark;
    }

    public void setFeeRemark(String feeRemark) {
        this.feeRemark = feeRemark;
    }

    public String getConfirmRemark() {
        return confirmRemark;
    }

    public void setConfirmRemark(String confirmRemark) {
        this.confirmRemark = confirmRemark;
    }

    public String getOrderRemark() {
        return orderRemark;
    }

    public void setOrderRemark(String orderRemark) {
        this.orderRemark = orderRemark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(String bookedBy) {
        this.bookedBy = bookedBy;
    }

    public OffsetDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(OffsetDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }
}
