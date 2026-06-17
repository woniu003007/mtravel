package com.mtravel.platform.enterprise.expenseitem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业费用项目实体，对应 resource_projects 表。
 *
 * <p>费用项目按资源类型配置，例如景区的成人、儿童，酒店的标间、大床房。
 * 采购价格管理会按资源类型读取这里的启用项目，避免项目类型写死在前端。</p>
 */
@TableName("resource_projects")
public class EnterpriseExpenseItemEntity extends TenantSoftDeleteEntity {

    /** 资源类型，用于过滤不同业务场景下可选的项目类型。 */
    @TableField("resource_type")
    private String resourceType;

    /** 项目名称，例如成人、儿童、标间。 */
    @TableField("project_name")
    private String projectName;

    /** 是否纳入统计，true 表示后续成本和经营统计可以使用该项目。 */
    @TableField("statistics_enabled")
    private Boolean statisticsEnabled;

    /** 排序号，数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Boolean getStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(Boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
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
