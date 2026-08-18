package com.mtravel.platform.configuration.quote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 导游等级配置实体。
 */
@TableName("sales_quote_guide_levels")
public class SalesQuoteGuideLevelEntity extends TenantSoftDeleteEntity {

    /** 导游等级名称。 */
    @TableField("level_name")
    private String levelName;

    /** 排序号。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 等级状态。 */
    @TableField("status")
    private String status;

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
