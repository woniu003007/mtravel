package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import com.mtravel.platform.purchase.resource.material.typehandler.PostgreSqlJsonbStringTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 产品每日资源介绍素材快照实体，对应 sales_product_day_resource_introductions 表。
 *
 * <p>一个产品日资源可以组合多个资源介绍素材。正文、温馨提示、游览时间和索引版本在保存时快照，
 * 以保证后续修改资源主档不会改变已经设计好的产品。</p>
 */
@TableName("sales_product_day_resource_introductions")
public class SalesProductDayResourceIntroductionEntity extends TenantSoftDeleteEntity {

    @TableField("product_id")
    private Long productId;
    @TableField("day_resource_id")
    private Long dayResourceId;
    @TableField("resource_introduction_id")
    private Long resourceIntroductionId;
    @TableField("introduction_index_version")
    private Integer introductionIndexVersion;
    @TableField(value = "title_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String titleSnapshot;
    @TableField(value = "content_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String contentSnapshot;
    @TableField(value = "notice_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String noticeSnapshot;
    @TableField(value = "warm_tip_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String warmTipSnapshot;
    /** 扩展内容模块快照，避免源素材修改影响已编排产品。 */
    @TableField(value = "extension_blocks_snapshot", typeHandler = PostgreSqlJsonbStringTypeHandler.class, jdbcType = JdbcType.OTHER)
    private String extensionBlocksSnapshot;
    @TableField(value = "visit_duration_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String visitDurationSnapshot;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_by")
    private String createdBy;
    @TableField(value = "remark", updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getDayResourceId() { return dayResourceId; }
    public void setDayResourceId(Long dayResourceId) { this.dayResourceId = dayResourceId; }
    public Long getResourceIntroductionId() { return resourceIntroductionId; }
    public void setResourceIntroductionId(Long resourceIntroductionId) { this.resourceIntroductionId = resourceIntroductionId; }
    public Integer getIntroductionIndexVersion() { return introductionIndexVersion; }
    public void setIntroductionIndexVersion(Integer introductionIndexVersion) { this.introductionIndexVersion = introductionIndexVersion; }
    public String getTitleSnapshot() { return titleSnapshot; }
    public void setTitleSnapshot(String titleSnapshot) { this.titleSnapshot = titleSnapshot; }
    public String getContentSnapshot() { return contentSnapshot; }
    public void setContentSnapshot(String contentSnapshot) { this.contentSnapshot = contentSnapshot; }
    public String getNoticeSnapshot() { return noticeSnapshot; }
    public void setNoticeSnapshot(String noticeSnapshot) { this.noticeSnapshot = noticeSnapshot; }
    public String getWarmTipSnapshot() { return warmTipSnapshot; }
    public void setWarmTipSnapshot(String warmTipSnapshot) { this.warmTipSnapshot = warmTipSnapshot; }
    public String getExtensionBlocksSnapshot() { return extensionBlocksSnapshot; }
    public void setExtensionBlocksSnapshot(String extensionBlocksSnapshot) { this.extensionBlocksSnapshot = extensionBlocksSnapshot; }
    public String getVisitDurationSnapshot() { return visitDurationSnapshot; }
    public void setVisitDurationSnapshot(String visitDurationSnapshot) { this.visitDurationSnapshot = visitDurationSnapshot; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
