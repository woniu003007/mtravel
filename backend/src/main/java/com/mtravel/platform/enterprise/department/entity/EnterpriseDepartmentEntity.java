package com.mtravel.platform.enterprise.department.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业部门实体。
 *
 * <p>部门是企业资料中的组织架构基础数据，员工、角色权限、计调归属和统计口径都会引用它。
 * 因此部门删除采用软删除，历史业务数据仍可保留部门名称和归属关系。</p>
 */
@TableName("enterprise_departments")
public class EnterpriseDepartmentEntity extends TenantSoftDeleteEntity {

    /** 上级部门 ID。为空表示一级部门。 */
    @TableField("parent_id")
    private Long parentId;

    /** 部门编码，用于内部识别或外部数据导入匹配。 */
    @TableField("department_code")
    private String departmentCode;

    /** 部门名称，例如销售部、计调部、财务部。 */
    @TableField("department_name")
    private String departmentName;

    /** 部门负责人姓名。 */
    @TableField("manager_name")
    private String managerName;

    /** 部门负责人对应的企业员工 ID，用于审批流按账号路由。 */
    @TableField("manager_employee_id")
    private Long managerEmployeeId;

    /** 部门联系电话或负责人联系电话。 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 排序值。数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 部门状态。active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Long getManagerEmployeeId() {
        return managerEmployeeId;
    }

    public void setManagerEmployeeId(Long managerEmployeeId) {
        this.managerEmployeeId = managerEmployeeId;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
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
