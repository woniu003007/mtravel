package com.mtravel.platform.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 系统用户实体。
 *
 * <p>对应 system_users 表，用于保存租户后台登录账号、密码哈希、基础角色和账号状态。
 * 当前只承载登录认证和粗粒度角色，完整角色权限关系后续独立建表。</p>
 */
@TableName("system_users")
public class SystemUserEntity {

    /** 系统用户主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID，标识该账号属于哪一家地接公司。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 登录账号，同一租户内未删除账号必须唯一。 */
    @TableField("username")
    private String username;

    /** 登录密码哈希，只保存 BCrypt 等安全哈希值，不能保存明文密码。 */
    @TableField("password_hash")
    private String passwordHash;

    /** 用户真实姓名，用于页面显示、操作日志和业务单据留痕。 */
    @TableField("real_name")
    private String realName;

    /** 用户手机号，可用于联系、通知或后续登录验证。 */
    @TableField("mobile_phone")
    private String mobilePhone;

    /** 用户邮箱，可用于通知或后续找回密码。 */
    @TableField("email")
    private String email;

    /** 企业角色 ID，用于关联账号当前归属的业务角色。 */
    @TableField("role_id")
    private Long roleId;

    /** 企业员工 ID，用于关联账号对应的员工资料。 */
    @TableField("employee_id")
    private Long employeeId;

    /** 角色编码，用于登录令牌和粗粒度权限识别。 */
    @TableField("role_code")
    private String roleCode;

    /** 是否为租户管理员，租户管理员可维护本租户基础设置。 */
    @TableField("is_tenant_admin")
    private Boolean isTenantAdmin;

    /** 账号状态，active 可登录，disabled 停用，locked 锁定。 */
    @TableField("status")
    private String status;

    /** 最后登录时间，用于账号审计和后续安全提醒。 */
    @TableField("last_login_at")
    private OffsetDateTime lastLoginAt;

    /** 账号备注，用于记录账号管理说明。 */
    @TableField("remark")
    private String remark;

    /** 创建人账号或名称。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /** 更新时间，由数据库触发器维护。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    /** 是否已软删除，登录和业务查询只允许读取 false 的账号。 */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /** 软删除时间。 */
    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    /** 执行软删除的账号或名称。 */
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Boolean getIsTenantAdmin() {
        return isTenantAdmin;
    }

    public void setIsTenantAdmin(Boolean tenantAdmin) {
        isTenantAdmin = tenantAdmin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
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
