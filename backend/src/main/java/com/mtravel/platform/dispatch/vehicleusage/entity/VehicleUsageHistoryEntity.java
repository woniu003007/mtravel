package com.mtravel.platform.dispatch.vehicleusage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.OffsetDateTime;

/**
 * 用车历史候选实体。
 *
 * <p>保存司机信息和车牌号的手动输入历史，用于下次输入时按使用次数推荐。</p>
 */
@TableName("vehicle_usage_histories")
public class VehicleUsageHistoryEntity extends TenantSoftDeleteEntity {

    /** 候选类型，driver_info 表示司机信息，vehicle_plate 表示车牌号。 */
    @TableField("history_type")
    private String historyType;

    /** 页面展示的原始候选内容。 */
    @TableField("content")
    private String content;

    /** 归一化后的内容，用于同租户同类型去重。 */
    @TableField("normalized_content")
    private String normalizedContent;

    /** 使用次数，保存一次有效用车安排后累加一次。 */
    @TableField("usage_count")
    private Integer usageCount;

    /** 最近一次使用时间，用于同次数时排序。 */
    @TableField("last_used_at")
    private OffsetDateTime lastUsedAt;

    public String getHistoryType() {
        return historyType;
    }

    public void setHistoryType(String historyType) {
        this.historyType = historyType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNormalizedContent() {
        return normalizedContent;
    }

    public void setNormalizedContent(String normalizedContent) {
        this.normalizedContent = normalizedContent;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
