package com.mtravel.platform.purchase.resource.material.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceStatus;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionResponse;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionReorderRequest;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionSaveRequest;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionImageEntity;
import com.mtravel.platform.purchase.resource.material.enums.PurchaseResourceIntroductionIndexStatus;
import com.mtravel.platform.purchase.resource.material.enums.PurchaseResourceIntroductionStatus;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionChunkMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/** 采购资源介绍素材服务，负责正文、温馨提示、注意事项、游览时间和向量索引生命周期。 */
@Service
public class PurchaseResourceIntroductionService {

    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseResourceIntroductionMapper introductionMapper;
    private final PurchaseResourceIntroductionChunkMapper chunkMapper;
    private final PurchaseResourceIntroductionProcessor processor;
    private final ResourceMaterialTagCodec tagCodec;
    private final ResourceIntroductionExtensionBlockCodec extensionBlockCodec;
    private final PurchaseResourceOptionalItemMapper optionalItemMapper;

    @Autowired(required = false)
    private PurchaseResourceIntroductionImageMapper introductionImageMapper;

    @Autowired
    public PurchaseResourceIntroductionService(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceIntroductionChunkMapper chunkMapper,
            PurchaseResourceIntroductionProcessor processor,
            ResourceMaterialTagCodec tagCodec,
            ResourceIntroductionExtensionBlockCodec extensionBlockCodec,
            PurchaseResourceOptionalItemMapper optionalItemMapper
    ) {
        this.resourceMapper = resourceMapper;
        this.introductionMapper = introductionMapper;
        this.chunkMapper = chunkMapper;
        this.processor = processor;
        this.tagCodec = tagCodec;
        this.extensionBlockCodec = extensionBlockCodec;
        this.optionalItemMapper = optionalItemMapper;
    }

    /** 兼容未接入资源自费项目主档前的测试和调用方。 */
    public PurchaseResourceIntroductionService(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceIntroductionChunkMapper chunkMapper,
            PurchaseResourceIntroductionProcessor processor,
            ResourceMaterialTagCodec tagCodec
    ) { this(resourceMapper, introductionMapper, chunkMapper, processor, tagCodec,
            new ResourceIntroductionExtensionBlockCodec(new com.fasterxml.jackson.databind.ObjectMapper()), null); }

    /** 兼容已有调用方的完整构造契约。 */
    public PurchaseResourceIntroductionService(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceIntroductionChunkMapper chunkMapper,
            PurchaseResourceIntroductionProcessor processor,
            ResourceMaterialTagCodec tagCodec,
            PurchaseResourceOptionalItemMapper optionalItemMapper
    ) { this(resourceMapper, introductionMapper, chunkMapper, processor, tagCodec,
            new ResourceIntroductionExtensionBlockCodec(new com.fasterxml.jackson.databind.ObjectMapper()), optionalItemMapper); }

    /** 查询当前租户资源下的介绍版本，草稿也返回供编辑使用。 */
    public List<PurchaseResourceIntroductionResponse> list(Long tenantId, Long resourceId) {
        validateResource(tenantId, resourceId);
        return introductionMapper.selectList(new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 保存完整的素材排序，拒绝缺项、跨资源 ID 和重复 ID，避免拖拽结果覆盖其它素材。
     */
    @Transactional
    public List<PurchaseResourceIntroductionResponse> reorder(
            Long tenantId,
            Long resourceId,
            PurchaseResourceIntroductionReorderRequest request
    ) {
        lockResource(tenantId, resourceId);
        List<PurchaseResourceIntroductionEntity> current = introductionMapper.selectList(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id")
                        .last("FOR UPDATE")
        );
        List<Long> requestedIds = request.introductionIds();
        Set<Long> requestedIdSet = new HashSet<>(requestedIds);
        if (requestedIdSet.size() != requestedIds.size()) {
            throw new BizException("介绍素材排序不能包含重复项");
        }
        Set<Long> currentIdSet = current.stream()
                .map(PurchaseResourceIntroductionEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (current.size() != requestedIds.size() || !currentIdSet.equals(requestedIdSet)) {
            throw new BizException("介绍素材排序必须包含当前资源全部未删除素材");
        }

        for (int index = 0; index < requestedIds.size(); index++) {
            // 排序是局部更新，不能带着实体中的空注意事项等字段一起更新。
            introductionMapper.update(null, baseUpdate(tenantId, resourceId, requestedIds.get(index))
                    .set("sort_order", index + 1));
        }
        return list(tenantId, resourceId);
    }

    /** 新增介绍草稿；草稿不创建向量切片。 */
    @Transactional
    public PurchaseResourceIntroductionResponse create(
            Long tenantId,
            Long resourceId,
            PurchaseResourceIntroductionSaveRequest request,
            String operator
    ) {
        lockResource(tenantId, resourceId);
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setTenantId(tenantId);
        entity.setResourceId(resourceId);
        entity.setIsOptionalItem(Boolean.TRUE.equals(request.isOptionalItem()));
        entity.setResourceOptionalItemId(validateOptionalItem(tenantId, resourceId, request));
        entity.setTitle(request.title().trim());
        entity.setTags(tagCodec.encode(request.tags()));
        entity.setContent(request.content().trim());
        entity.setNoticeContent(normalizeNoticeContent(request.noticeContent()));
        entity.setWarmTipContent(normalizeWarmTipContent(request.warmTipContent()));
        entity.setExtensionBlocks(extensionBlockCodec.encode(request.extensionBlocks()));
        entity.setVisitDuration(normalizeVisitDuration(request.visitDuration()));
        entity.setStatus(PurchaseResourceIntroductionStatus.DRAFT.value());
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(1);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        entity.setSortOrder(nextSortOrder(tenantId, resourceId));
        introductionMapper.insert(entity);
        return toResponse(entity);
    }

    /** 保存介绍草稿；已发布内容修改后必须重新发布才能再次进入推荐和检索。 */
    @Transactional
    public PurchaseResourceIntroductionResponse update(
            Long tenantId,
            Long resourceId,
            Long introductionId,
            PurchaseResourceIntroductionSaveRequest request
    ) {
        lockResource(tenantId, resourceId);
        PurchaseResourceIntroductionEntity existing = load(tenantId, resourceId, introductionId);
        int nextVersion = nextVersion(existing.getIndexVersion());
        Long resourceOptionalItemId = validateOptionalItem(tenantId, resourceId, request);
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setIsOptionalItem(Boolean.TRUE.equals(request.isOptionalItem()));
        entity.setTitle(request.title().trim());
        entity.setTags(tagCodec.encode(request.tags()));
        entity.setContent(request.content().trim());
        entity.setNoticeContent(normalizeNoticeContent(request.noticeContent()));
        entity.setWarmTipContent(normalizeWarmTipContent(request.warmTipContent()));
        entity.setExtensionBlocks(extensionBlockCodec.encode(request.extensionBlocks()));
        entity.setVisitDuration(normalizeVisitDuration(request.visitDuration()));
        entity.setStatus(PurchaseResourceIntroductionStatus.DRAFT.value());
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(nextVersion);
        entity.setErrorMessage(null);
        entity.setPublishedAt(null);
        // 只有编辑素材类型时才显式改关联项目；发布、重试等局部更新不能把它覆盖为空。
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId)
                .set("resource_optional_item_id", resourceOptionalItemId));
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
        return toResponse(load(tenantId, resourceId, introductionId));
    }

    /** 发布介绍并在事务提交后异步生成正文和温馨提示向量。 */
    @Transactional
    public PurchaseResourceIntroductionResponse publish(Long tenantId, Long resourceId, Long introductionId) {
        lockResource(tenantId, resourceId);
        PurchaseResourceIntroductionEntity existing = load(tenantId, resourceId, introductionId);
        if (!StringUtils.hasText(existing.getContent())) {
            throw new BizException("介绍正文不能为空");
        }
        int nextVersion = nextVersion(existing.getIndexVersion());
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setStatus(PurchaseResourceIntroductionStatus.PUBLISHED.value());
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(nextVersion);
        entity.setErrorMessage(null);
        entity.setPublishedAt(OffsetDateTime.now());
        // notice/warmTip/visitDuration 允许编辑时清空，因此使用 ALWAYS 更新策略。
        // 发布属于局部状态变更，必须显式保留这些素材内容，不能覆盖为空。
        entity.setNoticeContent(existing.getNoticeContent());
        entity.setWarmTipContent(existing.getWarmTipContent());
        entity.setVisitDuration(existing.getVisitDuration());
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId));
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
        registerAfterCommit(() -> processor.processAsync(tenantId, introductionId, nextVersion));
        return toResponse(load(tenantId, resourceId, introductionId));
    }

    /** 重试已发布介绍的向量化。 */
    @Transactional
    public PurchaseResourceIntroductionResponse retry(Long tenantId, Long resourceId, Long introductionId) {
        lockResource(tenantId, resourceId);
        PurchaseResourceIntroductionEntity existing = load(tenantId, resourceId, introductionId);
        if (!PurchaseResourceIntroductionStatus.PUBLISHED.value().equals(existing.getStatus())) {
            throw new BizException("只有已发布介绍才能重试向量化");
        }
        int nextVersion = nextVersion(existing.getIndexVersion());
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(nextVersion);
        entity.setErrorMessage(null);
        // 重试向量化同样只更新索引状态，保留已发布素材的内容和发布时间。
        entity.setNoticeContent(existing.getNoticeContent());
        entity.setWarmTipContent(existing.getWarmTipContent());
        entity.setVisitDuration(existing.getVisitDuration());
        entity.setPublishedAt(existing.getPublishedAt());
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId));
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
        registerAfterCommit(() -> processor.processAsync(tenantId, introductionId, nextVersion));
        return toResponse(load(tenantId, resourceId, introductionId));
    }

    /** 软删除介绍并立即物理删除对应文本切片和向量。 */
    @Transactional
    public void delete(Long tenantId, Long resourceId, Long introductionId, String operator) {
        lockResource(tenantId, resourceId);
        PurchaseResourceIntroductionEntity existing = load(tenantId, resourceId, introductionId);
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setStatus(PurchaseResourceIntroductionStatus.DISABLED.value());
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.DELETED.value());
        entity.setIndexVersion(nextVersion(existing.getIndexVersion()));
        entity.setErrorMessage("介绍素材已删除");
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId));
        if (introductionImageMapper != null) {
            PurchaseResourceIntroductionImageEntity deletedImageLink =
                    new PurchaseResourceIntroductionImageEntity();
            deletedImageLink.setIsDeleted(true);
            deletedImageLink.setDeletedAt(OffsetDateTime.now());
            deletedImageLink.setDeletedBy(operator);
            introductionImageMapper.update(deletedImageLink,
                    new UpdateWrapper<PurchaseResourceIntroductionImageEntity>()
                            .eq("tenant_id", tenantId)
                            .eq("introduction_id", introductionId)
                            .eq("is_deleted", false));
        }
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
    }

    private PurchaseResourceIntroductionEntity load(Long tenantId, Long resourceId, Long introductionId) {
        PurchaseResourceIntroductionEntity entity = introductionMapper.selectOne(new QueryWrapper<PurchaseResourceIntroductionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("id", introductionId)
                .last("limit 1"));
        if (entity == null) {
            throw new BizException("介绍素材不存在或已删除");
        }
        return entity;
    }

    private PurchaseResourceEntity validateResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .last("limit 1"));
        if (resource == null) {
            throw new BizException("资源不存在、已删除或已停用");
        }
        return resource;
    }

    /**
     * 介绍新增、删除和重排均先锁定资源主档，避免并发请求让完整排序校验通过后又插入遗漏记录。
     */
    private PurchaseResourceEntity lockResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .last("FOR UPDATE"));
        if (resource == null) {
            throw new BizException("资源不存在、已删除或已停用");
        }
        return resource;
    }

    /** 当前资源新增介绍始终追加到列表末尾，重排时已由资源锁与素材行锁保证一致性。 */
    private int nextSortOrder(Long tenantId, Long resourceId) {
        PurchaseResourceIntroductionEntity last = introductionMapper.selectOne(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .orderByDesc("sort_order")
                        .orderByDesc("id")
                        .last("LIMIT 1")
        );
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    /** 新版自费素材必须关联当前景区主档；保留旧字段兼容历史素材。 */
    private Long validateOptionalItem(Long tenantId, Long resourceId, PurchaseResourceIntroductionSaveRequest request) {
        if (!Boolean.TRUE.equals(request.isOptionalItem())) return null;
        if (request.resourceOptionalItemId() == null) throw new BizException("自费项目介绍必须关联自费项目");
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId).eq("id", resourceId).eq("is_deleted", false).last("limit 1"));
        if (resource == null || !"scenic".equals(resource.getResourceType())) throw new BizException("自费项目介绍仅适用于景区资源");
        if (optionalItemMapper == null) throw new BizException("自费项目主档服务未配置");
        PurchaseResourceOptionalItemEntity item = optionalItemMapper.selectOne(new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                .eq("tenant_id", tenantId).eq("resource_id", resourceId).eq("id", request.resourceOptionalItemId())
                .eq("is_deleted", false).last("limit 1"));
        if (item == null) throw new BizException("关联自费项目不属于当前景区或已删除");
        return item.getId();
    }

    private UpdateWrapper<PurchaseResourceIntroductionEntity> baseUpdate(Long tenantId, Long resourceId, Long introductionId) {
        return new UpdateWrapper<PurchaseResourceIntroductionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("id", introductionId);
    }

    private int nextVersion(Integer version) {
        return version == null || version < 1 ? 1 : version + 1;
    }

    /** 空白注意事项不落库，编辑时可通过空字符串清空已有内容。 */
    private String normalizeNoticeContent(String noticeContent) {
        return StringUtils.hasText(noticeContent) ? noticeContent.trim() : null;
    }

    private String normalizeWarmTipContent(String warmTipContent) {
        return StringUtils.hasText(warmTipContent) ? warmTipContent.trim() : null;
    }

    /** 空白游览时间不落库；有值时只允许保存分钟数字，编辑时可通过空字符串清空已有内容。 */
    private String normalizeVisitDuration(String visitDuration) {
        if (!StringUtils.hasText(visitDuration)) {
            return null;
        }
        String normalized = visitDuration.trim();
        if (!normalized.matches("\\d{1,6}")) {
            throw new BizException("游览时间只能填写分钟数字");
        }
        return normalized;
    }

    private PurchaseResourceIntroductionResponse toResponse(PurchaseResourceIntroductionEntity entity) {
        String name = null;
        if (entity.getResourceOptionalItemId() != null && optionalItemMapper != null) {
            PurchaseResourceOptionalItemEntity item = optionalItemMapper.selectById(entity.getResourceOptionalItemId());
            name = item == null ? null : item.getProjectName();
        }
        return PurchaseResourceIntroductionResponse.fromEntity(
                entity, tagCodec.decode(entity.getTags()), extensionBlockCodec.decode(entity.getExtensionBlocks()), name
        );
    }

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
