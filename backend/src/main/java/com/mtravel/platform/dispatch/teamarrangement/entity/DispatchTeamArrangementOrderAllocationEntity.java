package com.mtravel.platform.dispatch.teamarrangement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 正式团队安排成本订单归属实体。
 *
 * <p>该表保存团队公共成本、单订单成本和多订单均摊拆分结果，是订单毛利和客户成本统计依据。</p>
 */
@TableName("dispatch_team_arrangement_order_allocations")
public class DispatchTeamArrangementOrderAllocationEntity extends TenantSoftDeleteEntity {

    /** 所属安排 ID。 */
    @TableField("arrangement_id")
    private Long arrangementId;

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团队编号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 归属范围：team 团队公共，order 订单归属。 */
    @TableField("allocation_scope")
    private String allocationScope;

    /** 订单 ID。团队公共成本为空。 */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号快照。 */
    @TableField("order_no")
    private String orderNo;

    /** 客户 ID 快照。 */
    @TableField("customer_id")
    private Long customerId;

    /** 客户名称快照。 */
    @TableField("customer_name")
    private String customerName;

    /** 订单人数快照。 */
    @TableField("guest_count")
    private Integer guestCount;

    /** 成本归属模式。 */
    @TableField("allocation_mode")
    private String allocationMode;

    /** 多订单均摊方式。 */
    @TableField("split_mode")
    private String splitMode;

    /** 多订单拆分批次号。 */
    @TableField("split_batch_no")
    private String splitBatchNo;

    /** 原始录入金额。 */
    @TableField("original_amount")
    private BigDecimal originalAmount;

    /** 当前归属金额。 */
    @TableField("allocation_amount")
    private BigDecimal allocationAmount;

    /** 排序号。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getArrangementId() { return arrangementId; }
    public void setArrangementId(Long arrangementId) { this.arrangementId = arrangementId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public String getAllocationScope() { return allocationScope; }
    public void setAllocationScope(String allocationScope) { this.allocationScope = allocationScope; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public String getAllocationMode() { return allocationMode; }
    public void setAllocationMode(String allocationMode) { this.allocationMode = allocationMode; }
    public String getSplitMode() { return splitMode; }
    public void setSplitMode(String splitMode) { this.splitMode = splitMode; }
    public String getSplitBatchNo() { return splitBatchNo; }
    public void setSplitBatchNo(String splitBatchNo) { this.splitBatchNo = splitBatchNo; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getAllocationAmount() { return allocationAmount; }
    public void setAllocationAmount(BigDecimal allocationAmount) { this.allocationAmount = allocationAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
