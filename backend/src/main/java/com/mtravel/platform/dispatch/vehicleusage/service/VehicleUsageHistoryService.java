package com.mtravel.platform.dispatch.vehicleusage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.dispatch.vehicleusage.dto.VehicleUsageHistoryRecordRequest;
import com.mtravel.platform.dispatch.vehicleusage.dto.VehicleUsageHistoryResponse;
import com.mtravel.platform.dispatch.vehicleusage.entity.VehicleUsageHistoryEntity;
import com.mtravel.platform.dispatch.vehicleusage.enums.VehicleUsageHistoryType;
import com.mtravel.platform.dispatch.vehicleusage.mapper.VehicleUsageHistoryMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用车历史候选服务。
 *
 * <p>该服务只沉淀手动输入历史和使用次数，不做司机档案、车辆档案或供应商档案维护。</p>
 */
@Service
public class VehicleUsageHistoryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final VehicleUsageHistoryMapper mapper;

    public VehicleUsageHistoryService(VehicleUsageHistoryMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询历史候选。
     *
     * @param historyType 候选类型
     * @param keyword 搜索关键字，可为空
     * @param tenantId 当前租户 ID
     * @param limit 返回数量上限
     * @return 按使用次数和最近使用时间排序的候选
     */
    public List<VehicleUsageHistoryResponse> suggest(String historyType, String keyword, Long tenantId, Integer limit) {
        String typeValue = VehicleUsageHistoryType.fromValue(historyType).getValue();
        int queryLimit = normalizeLimit(limit);
        QueryWrapper<VehicleUsageHistoryEntity> wrapper = new QueryWrapper<VehicleUsageHistoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("history_type", typeValue)
                .like(StringUtils.hasText(keyword), "content", keyword == null ? null : keyword.trim())
                .orderByDesc("usage_count")
                .orderByDesc("last_used_at")
                .orderByAsc("content")
                .last("LIMIT " + queryLimit);
        return mapper.selectList(wrapper).stream()
                .map(VehicleUsageHistoryResponse::fromEntity)
                .toList();
    }

    /**
     * 记录一次候选使用。
     *
     * <p>同租户、同类型、同归一化内容只保留一条记录；再次使用时只增加次数和更新时间。</p>
     *
     * @param request 使用记录请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     */
    @Transactional
    public void recordUse(VehicleUsageHistoryRecordRequest request, Long tenantId, String operator) {
        String typeValue = VehicleUsageHistoryType.fromValue(request.historyType()).getValue();
        String content = clean(request.content());
        if (!StringUtils.hasText(content)) {
            return;
        }
        String normalizedContent = normalizeContent(content);
        VehicleUsageHistoryEntity existing = mapper.selectOne(new QueryWrapper<VehicleUsageHistoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("history_type", typeValue)
                .eq("normalized_content", normalizedContent));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing == null) {
            VehicleUsageHistoryEntity entity = new VehicleUsageHistoryEntity();
            entity.setTenantId(tenantId);
            entity.setHistoryType(typeValue);
            entity.setContent(content);
            entity.setNormalizedContent(normalizedContent);
            entity.setUsageCount(1);
            entity.setLastUsedAt(now);
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            mapper.insert(entity);
            return;
        }
        existing.setContent(content);
        existing.setUsageCount((existing.getUsageCount() == null ? 0 : existing.getUsageCount()) + 1);
        existing.setLastUsedAt(now);
        mapper.updateById(existing);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeContent(String value) {
        return value.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
