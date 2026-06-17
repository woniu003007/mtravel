package com.mtravel.platform.customer.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 客户分类实体，对应 customer_categories 表。
 *
 * <p>客户分类是客户主档的基础字典，例如 A 类客户、B 类客户、旅行社、单位客户等。
 * 业务上它不是订单数据，而是客户资料的分类维度，因此删除时必须做软删除，
 * 避免历史客户、订单、统计数据失去分类含义。</p>
 */
@TableName("customer_categories")
public class CustomerCategoryEntity {

    /** 客户分类主键 ID，由 PostgreSQL 自增序列生成。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID，用于隔离不同地接公司的客户分类数据。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户分类名称，同一租户下未删除分类名称必须唯一。 */
    @TableField("category_name")
    private String categoryName;

    /** 默认授信额度。客户选择该分类时可带入客户主档，客户保存后可单独调整。 */
    @TableField("default_credit_limit")
    private BigDecimal defaultCreditLimit;

    /** 排序号，数字越小越靠前，用于列表和下拉框展示。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 分类状态：active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    /** 创建人账号或名称，用于基础资料维护留痕。 */
    @TableField("created_by")
    private String createdBy;

    /** 分类备注，用于记录分类规则或内部管理说明。 */
    @TableField("remark")
    private String remark;

    /** 创建时间，由数据库默认值维护。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /** 更新时间，由数据库触发器维护。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    /** 是否已删除。true 表示软删除，常规查询必须过滤掉。 */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /** 删除时间。未删除时为空。 */
    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    /** 删除人账号或名称。未删除时为空。 */
    @TableField("deleted_by")
    private String deletedBy;

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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getDefaultCreditLimit() {
        return defaultCreditLimit;
    }

    public void setDefaultCreditLimit(BigDecimal defaultCreditLimit) {
        this.defaultCreditLimit = defaultCreditLimit;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }
}
