package com.mtravel.platform.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.OffsetDateTime;

/**
 * 多租户软删除业务实体基类。
 *
 * <p>新增业务表都需要 tenant_id 和软删除字段。把这些字段集中到基类，可以保证新增模块
 * 在查询、更新、删除时使用同一套字段含义，避免某个模块遗漏租户边界或误做物理删除。</p>
 */
public abstract class TenantSoftDeleteEntity {

    /** 业务记录主键 ID，由 PostgreSQL 自增序列生成。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID，用于隔离不同地接公司的业务数据。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 创建人账号或名称，用于业务资料维护留痕。 */
    @TableField("created_by")
    private String createdBy;

    /** 备注，用于记录业务说明。 */
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
