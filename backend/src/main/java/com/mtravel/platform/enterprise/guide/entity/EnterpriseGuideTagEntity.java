package com.mtravel.platform.enterprise.guide.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业导游标签实体。
 *
 * <p>导游标签用于描述导游能力、擅长线路、带团类型或内部分类，后续排团筛选和导游统计可按标签汇总。</p>
 */
@TableName("enterprise_guide_tags")
public class EnterpriseGuideTagEntity extends TenantSoftDeleteEntity {

    /** 标签名称，例如金牌导游、研学、亲子、英语。 */
    @TableField("tag_name")
    private String tagName;

    /** 标签状态。active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    /** 排序值。数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
