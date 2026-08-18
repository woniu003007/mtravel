package com.mtravel.platform.customer.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 客户授信超额审批抄送快照实体。
 *
 * <p>只有整张审批单最终通过后才写入 visibleAt，避免未完成流程提前出现在抄送列表。</p>
 */
@TableName("customer_risk_approval_ccs")
public class CustomerRiskApprovalCcEntity {

    /** 抄送记录主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户风控审批申请 ID。 */
    @TableField("request_id")
    private Long requestId;

    /** 抄送人的系统用户 ID。 */
    @TableField("cc_user_id")
    private Long ccUserId;

    /** 抄送人姓名快照。 */
    @TableField("cc_name")
    private String ccName;

    /** 抄送可见时间，最终通过前为空。 */
    @TableField("visible_at")
    private OffsetDateTime visibleAt;

    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getCcUserId() { return ccUserId; }
    public void setCcUserId(Long ccUserId) { this.ccUserId = ccUserId; }
    public String getCcName() { return ccName; }
    public void setCcName(String ccName) { this.ccName = ccName; }
    public OffsetDateTime getVisibleAt() { return visibleAt; }
    public void setVisibleAt(OffsetDateTime visibleAt) { this.visibleAt = visibleAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
