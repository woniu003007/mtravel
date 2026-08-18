package com.mtravel.platform.purchase.resource.material.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceStatus;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionResponse;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionSaveRequest;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.enums.PurchaseResourceIntroductionIndexStatus;
import com.mtravel.platform.purchase.resource.material.enums.PurchaseResourceIntroductionStatus;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionChunkMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/** 采购资源介绍素材服务，负责多版本正文、注意事项、发布状态和向量索引生命周期。 */
@Service
public class PurchaseResourceIntroductionService {

    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseResourceIntroductionMapper introductionMapper;
    private final PurchaseResourceIntroductionChunkMapper chunkMapper;
    private final PurchaseResourceIntroductionProcessor processor;
    private final ResourceMaterialTagCodec tagCodec;

    public PurchaseResourceIntroductionService(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceIntroductionChunkMapper chunkMapper,
            PurchaseResourceIntroductionProcessor processor,
            ResourceMaterialTagCodec tagCodec
    ) {
        this.resourceMapper = resourceMapper;
        this.introductionMapper = introductionMapper;
        this.chunkMapper = chunkMapper;
        this.processor = processor;
        this.tagCodec = tagCodec;
    }

    /** 查询当前租户资源下的介绍版本，草稿也返回供编辑使用。 */
    public List<PurchaseResourceIntroductionResponse> list(Long tenantId, Long resourceId) {
        validateResource(tenantId, resourceId);
        return introductionMapper.selectList(new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .orderByDesc("updated_at")
                        .orderByDesc("id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 新增介绍草稿；草稿不创建向量切片。 */
    @Transactional
    public PurchaseResourceIntroductionResponse create(
            Long tenantId,
            Long resourceId,
            PurchaseResourceIntroductionSaveRequest request,
            String operator
    ) {
        validateResource(tenantId, resourceId);
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setTenantId(tenantId);
        entity.setResourceId(resourceId);
        entity.setTitle(request.title().trim());
        entity.setTags(tagCodec.encode(request.tags()));
        entity.setContent(request.content().trim());
        entity.setNoticeContent(normalizeNoticeContent(request.noticeContent()));
        entity.setStatus(PurchaseResourceIntroductionStatus.DRAFT.value());
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(1);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
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
        PurchaseResourceIntroductionEntity existing = load(tenantId, resourceId, introductionId);
        int nextVersion = nextVersion(existing.getIndexVersion());
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setTitle(request.title().trim());
        entity.setTags(tagCodec.encode(request.tags()));
        entity.setContent(request.content().trim());
        entity.setNoticeContent(normalizeNoticeContent(request.noticeContent()));
        entity.setStatus(PurchaseResourceIntroductionStatus.DRAFT.value());
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(nextVersion);
        entity.setErrorMessage(null);
        entity.setPublishedAt(null);
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId));
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
        return toResponse(load(tenantId, resourceId, introductionId));
    }

    /** 发布介绍并在事务提交后异步生成正文和注意事项向量。 */
    @Transactional
    public PurchaseResourceIntroductionResponse publish(Long tenantId, Long resourceId, Long introductionId) {
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
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId));
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
        registerAfterCommit(() -> processor.processAsync(tenantId, introductionId, nextVersion));
        return toResponse(load(tenantId, resourceId, introductionId));
    }

    /** 重试已发布介绍的向量化。 */
    @Transactional
    public PurchaseResourceIntroductionResponse retry(Long tenantId, Long resourceId, Long introductionId) {
        PurchaseResourceIntroductionEntity existing = load(tenantId, resourceId, introductionId);
        if (!PurchaseResourceIntroductionStatus.PUBLISHED.value().equals(existing.getStatus())) {
            throw new BizException("只有已发布介绍才能重试向量化");
        }
        int nextVersion = nextVersion(existing.getIndexVersion());
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setIndexStatus(PurchaseResourceIntroductionIndexStatus.PENDING.value());
        entity.setIndexVersion(nextVersion);
        entity.setErrorMessage(null);
        introductionMapper.update(entity, baseUpdate(tenantId, resourceId, introductionId));
        chunkMapper.deleteByIntroduction(tenantId, introductionId);
        registerAfterCommit(() -> processor.processAsync(tenantId, introductionId, nextVersion));
        return toResponse(load(tenantId, resourceId, introductionId));
    }

    /** 软删除介绍并立即物理删除对应文本切片和向量。 */
    @Transactional
    public void delete(Long tenantId, Long resourceId, Long introductionId, String operator) {
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

    private PurchaseResourceIntroductionResponse toResponse(PurchaseResourceIntroductionEntity entity) {
        return PurchaseResourceIntroductionResponse.fromEntity(entity, tagCodec.decode(entity.getTags()));
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
