package com.mtravel.platform.sales.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 销售团队编号生成日志实体，对应 sales_team_no_logs 表。
 *
 * <p>记录每次团号生成使用的日期和后缀，避免同日多团编号排查困难。</p>
 */
@TableName("sales_team_no_logs")
public class SalesTeamNoLogEntity {

    /** 日志主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 所属销售产品模板 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 发团日期。 */
    @TableField("departure_date")
    private LocalDate departureDate;

    /** 生成的团队编号。 */
    @TableField("team_no")
    private String teamNo;

    /** 团号后缀。 */
    @TableField("suffix_code")
    private String suffixCode;

    /** 操作人。 */
    @TableField("operator")
    private String operator;

    /** 生成时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /** 备注。 */
    @TableField("remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public String getTeamNo() {
        return teamNo;
    }

    public void setTeamNo(String teamNo) {
        this.teamNo = teamNo;
    }

    public String getSuffixCode() {
        return suffixCode;
    }

    public void setSuffixCode(String suffixCode) {
        this.suffixCode = suffixCode;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
