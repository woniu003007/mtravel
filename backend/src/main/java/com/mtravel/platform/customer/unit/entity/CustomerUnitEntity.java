package com.mtravel.platform.customer.unit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 客户单位实体，对应 customers 表。
 *
 * <p>客户单位是销售下单、客户合同、应收账款和后续授信提醒的基础主档。
 * 本实体只承载客户主档字段，不把应收、授信、合同状态混入客户状态字段，
 * 避免一个字段承担多种业务含义。</p>
 */
@TableName("customers")
public class CustomerUnitEntity {

    /** 客户单位主键 ID，由 PostgreSQL 自增序列生成。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID，用于隔离不同地接公司的客户资料。 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户编码或业务代码，同一租户下未删除客户非空编码必须唯一。 */
    @TableField("customer_code")
    private String customerCode;

    /** 客户单位名称，例如组团社、批发商、单位客户名称。 */
    @TableField("customer_name")
    private String customerName;

    /** 客户分类 ID，关联 customer_categories 表。 */
    @TableField("category_id")
    private Long categoryId;

    /** 客户实际授信额度，按客户独立维护。 */
    @TableField("credit_limit")
    private BigDecimal creditLimit;

    /** 客户所在地省份。 */
    @TableField("province")
    private String province;

    /** 客户所在地城市。 */
    @TableField("city")
    private String city;

    /** 客户所在地区县。 */
    @TableField("district")
    private String district;

    /** 归属部门 ID，用于关联企业部门资料；为空表示全公司可见或暂未分配。 */
    @TableField("department_id")
    private Long departmentId;

    /** 客户归属部门名称冗余字段，用于列表展示和历史数据兼容。 */
    @TableField("department_name")
    private String departmentName;

    /** 默认操作计调员工 ID，用于关联企业员工资料；为空表示未分配。 */
    @TableField("dispatcher_employee_id")
    private Long dispatcherEmployeeId;

    /** 默认操作计调姓名冗余字段，用于列表展示和历史数据兼容。 */
    @TableField("dispatcher_name")
    private String dispatcherName;

    /** 客户结款方式：不限、现结或 1 到 12 个月账期。 */
    @TableField("settlement_method")
    private String settlementMethod;

    /** 客户账单起始日期，用于计算应收账期和结款提醒。 */
    @TableField("bill_start_date")
    private LocalDate billStartDate;

    /** 客户约定结款日，取值 1 到 31；为空表示未约定固定结款日。 */
    @TableField("bill_day")
    private Integer billDay;

    /** 客户负责人或联系人姓名。 */
    @TableField("contact_name")
    private String contactName;

    /** 客户负责人或联系人电话。 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 登记人名称，用于保留客户资料录入责任人。 */
    @TableField("registrar_name")
    private String registrarName;

    /** 客户合同有效期止，用于排团或下单时提醒合同是否临期或过期。 */
    @TableField("contract_expire_date")
    private LocalDate contractExpireDate;

    /** 客户主档状态：active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    /** 客户备注，用于记录合作说明、特殊结算要求或内部备注。 */
    @TableField("remark")
    private String remark;

    /** 创建人账号或名称，用于基础资料维护留痕。 */
    @TableField("created_by")
    private String createdBy;

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

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
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

    public Long getDispatcherEmployeeId() {
        return dispatcherEmployeeId;
    }

    public void setDispatcherEmployeeId(Long dispatcherEmployeeId) {
        this.dispatcherEmployeeId = dispatcherEmployeeId;
    }

    public String getDispatcherName() {
        return dispatcherName;
    }

    public void setDispatcherName(String dispatcherName) {
        this.dispatcherName = dispatcherName;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public LocalDate getBillStartDate() {
        return billStartDate;
    }

    public void setBillStartDate(LocalDate billStartDate) {
        this.billStartDate = billStartDate;
    }

    public Integer getBillDay() {
        return billDay;
    }

    public void setBillDay(Integer billDay) {
        this.billDay = billDay;
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

    public String getRegistrarName() {
        return registrarName;
    }

    public void setRegistrarName(String registrarName) {
        this.registrarName = registrarName;
    }

    public LocalDate getContractExpireDate() {
        return contractExpireDate;
    }

    public void setContractExpireDate(LocalDate contractExpireDate) {
        this.contractExpireDate = contractExpireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
