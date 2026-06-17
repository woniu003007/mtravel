package com.mtravel.platform.enterprise.employee.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业员工实体。
 *
 * <p>员工资料承接部门、角色、登录账号和四类数据查看范围。员工删除采用软删除，避免历史业务单据失去人员归属。</p>
 */
@TableName("enterprise_employees")
public class EnterpriseEmployeeEntity extends TenantSoftDeleteEntity {

    /** 系统用户 ID，用于关联后台登录账号。 */
    @TableField("system_user_id")
    private Long systemUserId;

    /** 员工业务编码，用于业务归属、外部导入或内部识别。 */
    @TableField("employee_code")
    private String employeeCode;

    /** 员工姓名，用于业务页面、单据和操作留痕展示。 */
    @TableField("employee_name")
    private String employeeName;

    /** 登录账号冗余值，用于员工列表展示和账号查重。 */
    @TableField("username")
    private String username;

    /** 所属部门 ID。为空表示暂未分配部门。 */
    @TableField("department_id")
    private Long departmentId;

    /** 角色 ID。为空表示暂未分配角色。 */
    @TableField("role_id")
    private Long roleId;

    /** 性别。male 男，female 女，unknown 未填写。 */
    @TableField("gender")
    private String gender;

    /** 固定电话或办公室电话。 */
    @TableField("telephone")
    private String telephone;

    /** 手机号码。 */
    @TableField("mobile_phone")
    private String mobilePhone;

    /** 邮箱地址。 */
    @TableField("email")
    private String email;

    /** 信息查看范围。company 全公司，department 部门范围，personal 个人信息。 */
    @TableField("info_scope")
    private String infoScope;

    /** 利润查看范围。company 全公司，department 部门范围，personal 个人信息。 */
    @TableField("profit_scope")
    private String profitScope;

    /** 收客查看范围。company 全公司，department 部门范围，personal 个人信息。 */
    @TableField("reception_scope")
    private String receptionScope;

    /** 客户查看范围。company 全公司，department 部门范围，personal 个人信息。 */
    @TableField("customer_scope")
    private String customerScope;

    /** 排序值。数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 员工状态。active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public Long getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Long systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
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

    public String getInfoScope() {
        return infoScope;
    }

    public void setInfoScope(String infoScope) {
        this.infoScope = infoScope;
    }

    public String getProfitScope() {
        return profitScope;
    }

    public void setProfitScope(String profitScope) {
        this.profitScope = profitScope;
    }

    public String getReceptionScope() {
        return receptionScope;
    }

    public void setReceptionScope(String receptionScope) {
        this.receptionScope = receptionScope;
    }

    public String getCustomerScope() {
        return customerScope;
    }

    public void setCustomerScope(String customerScope) {
        this.customerScope = customerScope;
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
}
