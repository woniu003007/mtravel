package com.mtravel.platform.purchase.resource.material.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import com.mtravel.platform.purchase.resource.material.typehandler.PostgreSqlJsonbStringTypeHandler;
import java.time.OffsetDateTime;
import org.apache.ibatis.type.JdbcType;

/** 采购资源介绍素材实体，对应 purchase_resource_introductions 表。 */
@TableName(value = "purchase_resource_introductions", autoResultMap = true)
public class PurchaseResourceIntroductionEntity extends TenantSoftDeleteEntity {

    /** 资源主档 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 介绍素材名称。 */
    @TableField("title")
    private String title;

    /** JSON 数组格式的适用标签。 */
    @TableField(value = "tags", typeHandler = PostgreSqlJsonbStringTypeHandler.class, jdbcType = JdbcType.OTHER)
    private String tags;

    /** 介绍正文。 */
    @TableField("content")
    private String content;

    /** 介绍使用时需要重点提示的注意事项，一行一条。 */
    @TableField(value = "notice_content", updateStrategy = FieldStrategy.ALWAYS)
    private String noticeContent;

    /** 发布状态。 */
    @TableField("status")
    private String status;

    /** 向量索引状态。 */
    @TableField("index_status")
    private String indexStatus;

    /** 向量索引版本。 */
    @TableField("index_version")
    private Integer indexVersion;

    /** 最近一次索引失败原因。 */
    @TableField(value = "error_message", updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;

    /** 最近一次发布时间。 */
    @TableField(value = "published_at", updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime publishedAt;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getNoticeContent() { return noticeContent; }
    public void setNoticeContent(String noticeContent) { this.noticeContent = noticeContent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public Integer getIndexVersion() { return indexVersion; }
    public void setIndexVersion(Integer indexVersion) { this.indexVersion = indexVersion; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
}
