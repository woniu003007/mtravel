package com.mtravel.platform.enterprise.guide.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 导游与标签关系实体。
 *
 * <p>一个导游可以拥有多个标签，一个标签也可以绑定多个导游。关系表独立存在，避免把多值标签塞进导游主表。</p>
 */
@TableName("enterprise_guide_tag_relations")
public class EnterpriseGuideTagRelationEntity extends TenantSoftDeleteEntity {

    /** 导游档案 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 导游标签 ID。 */
    @TableField("tag_id")
    private Long tagId;

    public Long getGuideId() {
        return guideId;
    }

    public void setGuideId(Long guideId) {
        this.guideId = guideId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
